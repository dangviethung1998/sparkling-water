/*
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements.  See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License.  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package ai.h2o.sparkling.ml.features

import ai.h2o.sparkling.ml.utils.H2OParamsReadable
import org.apache.spark.annotation.DeveloperApi
import org.apache.spark.ml.Transformer
import org.apache.spark.ml.param._
import org.apache.spark.ml.util.{DefaultParamsWritable, Identifiable}
import org.apache.spark.sql.types.{StructField, StructType}
import org.apache.spark.sql.{DataFrame, Dataset}

/**
  * Column pruner removes specified columns in the input dataset
  */
class ColumnPruner(override val uid: String) extends Transformer with ColumnPrunerParams with DefaultParamsWritable {

  def this() = this(Identifiable.randomUID("columnPruner"))

  @DeveloperApi
  override def transformSchema(schema: StructType): StructType = {
    val columnsToLeft = if (getKeep()) {
      schema.fieldNames.filter(getColumns().contains(_))
    } else {
      schema.fieldNames.filter(!getColumns().contains(_))
    }

    StructType(columnsToLeft.map {
      col => StructField(col, schema(col).dataType, schema(col).nullable, schema(col).metadata)
    })
  }

  override def transform(dataset: Dataset[_]): DataFrame = {
    val columnsToRemove = if (getKeep()) {
      dataset.columns.filter(!getColumns().contains(_))
    } else {
      dataset.columns.filter(getColumns().contains(_))
    }
    var resultDataset = dataset
    columnsToRemove.foreach {
      col => resultDataset = resultDataset.drop(col)
    }
    resultDataset.toDF()
  }

  override def copy(extra: ParamMap): Transformer = defaultCopy(extra)
}

object ColumnPruner extends H2OParamsReadable[ColumnPruner]

trait ColumnPrunerParams extends Params {

  //
  // Param definitions
  //
  private final val keep = new BooleanParam(this, "keep", "Determines if the column specified in the 'columns' parameter should be kept or removed")
  private final val columns = new StringArrayParam(this, "columns", "List of columns to be kept or removed")

  //
  // Default values
  //
  setDefault(
    keep -> false, // default is false which means remove specified columns
    columns -> Array[String]() // default is empty array which means no columns are removed
  )

  //
  // Getters
  //
  def getKeep() = $(keep)

  def getColumns() = $(columns)

  //
  // Setters
  //
  def setKeep(value: Boolean): this.type = set(keep, value)

  def setColumns(value: Array[String]): this.type = set(columns, value)
}
