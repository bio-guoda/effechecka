package effechecka.repackaged.io.eels.component.parquet

import java.math.{BigInteger, MathContext}
import java.nio.{ByteBuffer, ByteOrder}
import java.sql.{Date, Timestamp}
import java.time.{LocalDateTime, ZoneId}

import effechecka.Logging
import effechecka.repackaged.io.eels.Row
import effechecka.repackaged.io.eels.schema._
import org.apache.hadoop.conf.Configuration
import org.apache.parquet.column.Dictionary
import org.apache.parquet.hadoop.api.ReadSupport
import org.apache.parquet.hadoop.api.ReadSupport.ReadContext
import org.apache.parquet.io.api._
import org.apache.parquet.schema.MessageType

// adapted from https://github.com/51zero/eel-sdk
// required by the parquet reader builder, and returns a record materializer for rows
class RowReadSupport extends ReadSupport[Row] with Logging {

  override def prepareForRead(configuration: Configuration,
                              keyValueMetaData: java.util.Map[String, String],
                              fileSchema: MessageType,
                              readContext: ReadContext): RecordMaterializer[Row] = {
    new RowRecordMaterializer(fileSchema, readContext)
  }

  override def init(configuration: Configuration,
                    keyValueMetaData: java.util.Map[String, String],
                    fileSchema: MessageType): ReadSupport.ReadContext = {
    val projectionSchemaString = configuration.get(ReadSupport.PARQUET_READ_SCHEMA)
    val requestedSchema = ReadSupport.getSchemaForRead(fileSchema, projectionSchemaString)
    logger.debug("Parquet requested schema: " + requestedSchema)
    new ReadSupport.ReadContext(requestedSchema)
  }
}

// a row materializer retrns a group converter which is invoked for each
// field in a group to get a converter for that field, and then each of those
// converts is in turn called with the basic value.
// The converter must know what to do with the basic value so where basic values
// overlap, eg byte arrays, you must have different converters
class RowRecordMaterializer(fileSchema: MessageType,
                            readContext: ReadContext) extends RecordMaterializer[Row] with Logging {

  val schema = ParquetSchemaFns.fromParquetMessageType(readContext.getRequestedSchema)
  logger.debug(s"Record materializer will create row with schema $schema")

  override val getRootConverter: StructConverter = new StructConverter(schema, -1, None)
  override def skipCurrentRecord(): Unit = getRootConverter.start()
  override def getCurrentRecord: Row = Row(schema, getRootConverter.builder.result)
}

object Converter {
  def apply(dataType: DataType, nullable: Boolean, fieldIndex: Int, builder: ValuesBuilder): Converter = {
    require(builder != null)
    dataType match {
      case ArrayType(elementType) => new ArrayConverter(elementType, fieldIndex, builder)
      case BinaryType => new DefaultPrimitiveConverter(fieldIndex, builder)
      case BooleanType => new DefaultPrimitiveConverter(fieldIndex, builder)
      case DateType => new DateConverter(fieldIndex, builder)
      case DecimalType(precision, scale) => new DecimalConverter(fieldIndex, builder, precision, scale)
      case DoubleType => new DefaultPrimitiveConverter(fieldIndex, builder)
      case FloatType => new DefaultPrimitiveConverter(fieldIndex, builder)
      case _: IntType => new DefaultPrimitiveConverter(fieldIndex, builder)
      case _: LongType => new DefaultPrimitiveConverter(fieldIndex, builder)
      case _: ShortType => new DefaultPrimitiveConverter(fieldIndex, builder)
      case mapType@MapType(keyType, valueType) => new MapConverter(fieldIndex, builder, mapType)
      case StringType => new StringConverter(fieldIndex, builder)
      case struct: StructType => new StructConverter(struct, fieldIndex, Option(builder))
      case TimestampMillisType => new TimestampConverter(fieldIndex, builder)
      case other => sys.error("Unsupported type " + other)
    }
  }
}

class StructConverter(schema: StructType, index: Int, parent: Option[ValuesBuilder]) extends GroupConverter with Logging {
  logger.debug(s"Creating group converter for $schema")

  // nested array for this group/struct
  val builder: ValuesBuilder = new ArrayBuilder(schema.size)

  private val converters = schema.fields.zipWithIndex.map {
    case (field, fieldIndex) => Converter(field.dataType, field.nullable, fieldIndex, builder)
  }

  override def getConverter(fieldIndex: Int): Converter = converters(fieldIndex)
  override def end(): Unit = parent.foreach(_.put(index, builder.result))
  override def start(): Unit = builder.reset()
}

class ArrayConverter(elementType: DataType,
                     index: Int,
                     parent: ValuesBuilder) extends GroupConverter with Logging {

  private val builder = new VectorBuilder()

  // the outer converter is just a pass through to the list converter
  override def getConverter(fieldIndex: Int): Converter = new GroupConverter {
    override def getConverter(fieldIndex: Int): Converter = Converter(elementType, false, -1, builder)
    override def start(): Unit = ()
    override def end(): Unit = () // a no-op as each nested group only contains a single element and we want to handle the finished list
  }
  override def start(): Unit = builder.reset()
  override def end(): Unit = parent.put(index, builder.result)
}

class MapConverter(index: Int,
                   parent: ValuesBuilder,
                   mapType: MapType) extends GroupConverter {

  private val keys = new VectorBuilder()
  private val values = new VectorBuilder()

  override def getConverter(fieldIndex: Int): Converter = new GroupConverter {
    override def getConverter(fieldIndex: Int): Converter = fieldIndex match {
      case 0 => Converter(mapType.keyType, false, -1, keys)
      case 1 => Converter(mapType.valueType, false, -1, values)
    }
    override def start(): Unit = ()
    override def end(): Unit = () // a no-op as each nested group only contains a single element and we want to handle the finished list
  }

  override def start(): Unit = {
    keys.reset()
    values.reset()
  }

  override def end(): Unit = {
    val map = keys.result.zip(values.result).toMap
    parent.put(index, map)
  }
}

// just adds the parquet type directly into the builder
// for types that are not pass through, create an instance of a more specialized converter
// we need the index so that we know which fields were present in the file as they will be skipped if null
class DefaultPrimitiveConverter(index: Int, builder: ValuesBuilder) extends PrimitiveConverter with Logging {
  override def addBinary(value: Binary): Unit = builder.put(index, value.getBytes)
  override def addDouble(value: Double): Unit = builder.put(index, value)
  override def addLong(value: Long): Unit = builder.put(index, value)
  override def addBoolean(value: Boolean): Unit = builder.put(index, value)
  override def addInt(value: Int): Unit = builder.put(index, value)
  override def addFloat(value: Float): Unit = builder.put(index, value)
}

class StringConverter(index: Int,
                      builder: ValuesBuilder) extends PrimitiveConverter with Logging {
  require(builder != null)

  private var dict: Array[String] = null

  override def addBinary(value: Binary): Unit = builder.put(index, value.toStringUsingUTF8)

  override def hasDictionarySupport: Boolean = true

  override def setDictionary(dictionary: Dictionary): Unit = {
    dict = new Array[String](dictionary.getMaxId + 1)
    for (k <- 0 to dictionary.getMaxId) {
      dict(k) = dictionary.decodeToBinary(k).toStringUsingUTF8
    }
  }

  override def addValueFromDictionary(dictionaryId: Int): Unit = builder.put(index, dict(dictionaryId))
}

// we must use the precision and scale to build the value back from the bytes
class DecimalConverter(index: Int,
                       builder: ValuesBuilder,
                       precision: Precision,
                       scale: Scale) extends PrimitiveConverter {
  override def addBinary(value: Binary): Unit = {
    val bi = new BigInteger(value.getBytes)
    val bd = BigDecimal.apply(bi, scale.value, new MathContext(precision.value))
    builder.put(index, bd)
  }
}

// https://github.com/Parquet/parquet-mr/issues/218
class TimestampConverter(index: Int, builder: ValuesBuilder) extends PrimitiveConverter {

  private val JulianEpochInGregorian = LocalDateTime.of(-4713, 11, 24, 0, 0, 0)

  override def addBinary(value: Binary): Unit = {
    // first 8 bytes is the nanoseconds
    // second 4 bytes are the days
    val nanos = ByteBuffer.wrap(value.getBytes.slice(0, 8)).order(ByteOrder.LITTLE_ENDIAN).getLong()
    val days = ByteBuffer.wrap(value.getBytes.slice(8, 12)).order(ByteOrder.LITTLE_ENDIAN).getInt()
    val dt = JulianEpochInGregorian.plusDays(days).plusNanos(nanos)
    val millis = dt.atZone(ZoneId.systemDefault).toInstant.toEpochMilli
    builder.put(index, new Timestamp(millis))
  }
}

class DateConverter(index: Int,
                    builder: ValuesBuilder) extends PrimitiveConverter {

  private val UnixEpoch = LocalDateTime.of(1970, 1, 1, 0, 0, 0)

  override def addInt(value: Int): Unit = {
    val dt = UnixEpoch.plusDays(value)
    val millis = dt.atZone(ZoneId.systemDefault).toInstant.toEpochMilli
    builder.put(index, new Date(millis))
  }
}

trait ValuesBuilder {
  def reset(): Unit
  def put(pos: Int, value: Any): Unit
  def result: Seq[Any]
}

class VectorBuilder extends ValuesBuilder with Logging {

  private var vector = Vector.newBuilder[Any]

  override def reset(): Unit = vector = Vector.newBuilder[Any]
  override def put(pos: Int, value: Any): Unit = {
    vector.+=(value)
  }
  override def result: Seq[Any] = vector.result()
}

class ArrayBuilder(size: Int) extends ValuesBuilder {

  private var array: Array[Any] = _
  reset()

  def result: Seq[Any] = array

  def reset() = array = Array.ofDim(size)
  def put(pos: Int, value: Any): Unit = array(pos) = value
}
