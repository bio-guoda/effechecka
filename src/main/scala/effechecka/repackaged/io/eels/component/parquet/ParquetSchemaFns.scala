package effechecka.repackaged.io.eels.component.parquet

import effechecka.repackaged.io.eels.schema.{StructType, _}
import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName
import org.apache.parquet.schema.Type.Repetition
import org.apache.parquet.schema._

import scala.collection.JavaConverters._

/**
  * adapted from https://github.com/51zero/eel-sdk
  * See parquet formats at https://github.com/Parquet/parquet-format/blob/master/LogicalTypes.md
  */
object ParquetSchemaFns {

  implicit class RichType(tpe: Type) {
    def isGroupType: Boolean = !tpe.isPrimitive
  }

  def fromParquetPrimitiveType(tpe: PrimitiveType): DataType = {
    val baseType = tpe.getPrimitiveTypeName match {
      case PrimitiveTypeName.BINARY =>
        tpe.getOriginalType match {
          case OriginalType.ENUM => EnumType(tpe.getName, Nil)
          case OriginalType.UTF8 => StringType
          case _ => BinaryType
        }
      case PrimitiveTypeName.BOOLEAN => BooleanType
      case PrimitiveTypeName.DOUBLE => DoubleType
      case PrimitiveTypeName.FLOAT => FloatType
      case PrimitiveTypeName.FIXED_LEN_BYTE_ARRAY =>
        tpe.getOriginalType match {
          case OriginalType.DECIMAL =>
            val meta = tpe.getDecimalMetadata
            DecimalType(Precision(meta.getPrecision), Scale(meta.getScale))
          case _ => BinaryType
        }
      case PrimitiveTypeName.INT32 =>
        tpe.getOriginalType match {
          case OriginalType.UINT_32 => IntType.Unsigned
          case OriginalType.UINT_16 => ShortType.Unsigned
          case OriginalType.UINT_8 => ShortType.Unsigned
          case OriginalType.INT_16 => ShortType.Signed
          case OriginalType.INT_8 => ShortType.Signed
          case OriginalType.TIME_MILLIS => TimeMillisType
          case OriginalType.DATE => DateType
          case OriginalType.DECIMAL =>
            val meta = tpe.getDecimalMetadata
            DecimalType(Precision(meta.getPrecision), Scale(meta.getScale))
          case _ => IntType.Signed
        }
      case PrimitiveTypeName.INT64 if tpe.getOriginalType == OriginalType.UINT_64 => IntType.Unsigned
      case PrimitiveTypeName.INT64 if tpe.getOriginalType == OriginalType.TIME_MICROS => TimeMicrosType
      case PrimitiveTypeName.INT64 if tpe.getOriginalType == OriginalType.TIMESTAMP_MILLIS => TimestampMillisType
      case PrimitiveTypeName.INT64 if tpe.getOriginalType == OriginalType.TIMESTAMP_MICROS => TimestampMicrosType
      case PrimitiveTypeName.INT64 if tpe.getOriginalType == OriginalType.DECIMAL => DecimalType(Precision(18), Scale(2))
      case PrimitiveTypeName.INT64 => LongType.Signed
      // https://github.com/Parquet/parquet-mr/issues/218
      case PrimitiveTypeName.INT96 => TimestampMillisType
      case other => sys.error("Unsupported type " + other)
    }
    if (tpe.isRepetition(Repetition.REPEATED)) ArrayType(baseType) else baseType
  }

  def fromParquetMessageType(messageType: MessageType): StructType = {
    val fields = messageType.getFields.asScala.map { tpe =>
      val dataType = fromParquetType(tpe)
      Field(tpe.getName, dataType, tpe.getRepetition == Repetition.OPTIONAL)
    }
    StructType(fields)
  }

  def fromParquetType(tpe: Type): DataType = {
    if (tpe.isPrimitive) {
      fromParquetPrimitiveType(tpe.asPrimitiveType)
      // a map must be a group, with key/value fields or tagged as map
    } else if (tpe.isInstanceOf[MessageType]) {
      fromParquetMessageType(tpe.asInstanceOf[MessageType])
    } else if (tpe.getOriginalType == OriginalType.MAP) {
      fromParquetMapType(tpe.asGroupType)
    } else if (tpe.getOriginalType == OriginalType.LIST) {
      fromParquetArrayType(tpe.asGroupType)
    } else {
      fromParquetGroupType(tpe.asGroupType)
    }
  }

  def fromParquetArrayType(gt: GroupType): ArrayType = {
    val elementType = fromParquetType(gt.getFields.get(0).asGroupType().getFields.get(0))
    ArrayType(elementType)
  }

  // if the parquet group has just two fields, key and value, then we assume its a map
  def fromParquetMapType(gt: GroupType): MapType = {
    val key_value = gt.getFields.get(0).asGroupType()
    val keyType = fromParquetType(key_value.getFields.get(0))
    val valueType = fromParquetType(key_value.getFields.get(1))
    MapType(keyType, valueType)
  }

  def fromParquetGroupType(gt: GroupType): DataType = {
    val fields = gt.getFields.asScala.map { tpe =>
      val dataType = fromParquetType(tpe)
      Field(tpe.getName, dataType, tpe.getRepetition == Repetition.OPTIONAL)
    }
    val struct = StructType(fields)
    if (gt.isRepetition(Repetition.REPEATED)) ArrayType(struct) else struct
  }

}

