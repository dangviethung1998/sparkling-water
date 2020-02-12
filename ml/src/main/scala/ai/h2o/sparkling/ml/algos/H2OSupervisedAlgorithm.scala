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
package ai.h2o.sparkling.ml.algos

import ai.h2o.sparkling.frame.H2OColumnType
import ai.h2o.sparkling.ml.models.H2OSupervisedMOJOModel
import ai.h2o.sparkling.ml.params.H2OAlgoSupervisedParams
import hex.Model
import hex.genmodel.utils.DistributionFamily
import org.apache.spark.annotation.DeveloperApi
import org.apache.spark.h2o.backends.external.RestApiUtils
import org.apache.spark.h2o.{Frame, H2OBaseModel, H2OBaseModelBuilder, H2OContext}
import org.apache.spark.sql.Dataset
import org.apache.spark.sql.types.StructType
import water.DKV

import scala.reflect.ClassTag

abstract class H2OSupervisedAlgorithm[B <: H2OBaseModelBuilder : ClassTag, M <: H2OBaseModel, P <: Model.Parameters : ClassTag]
  extends H2OAlgorithm[B, M, P] with H2OAlgoSupervisedParams[P] {

  @DeveloperApi
  override def transformSchema(schema: StructType): StructType = {
    require(schema.fields.exists(f => f.name.compareToIgnoreCase(getLabelCol()) == 0),
      s"Specified label column '${getLabelCol()} was not found in input dataset!")
    require(!getFeaturesCols().exists(n => n.compareToIgnoreCase(getLabelCol()) == 0),
      "Specified input features cannot contain the label column!")
    require(getWeightCol() == null || getWeightCol() != getFoldCol(),
      "Specified weight column cannot be the same as the fold column!")
    require(getOffsetCol() == null || getOffsetCol() != getFoldCol(),
      "Specified offset column cannot be the same as the fold column!")
    schema
  }

  override protected def preProcessBeforeFit(trainFrameKey: String): Unit = {
    super.preProcessBeforeFit(trainFrameKey)
    if (parameters._distribution == DistributionFamily.bernoulli || parameters._distribution == DistributionFamily.multinomial) {
      val hc = H2OContext.ensure()
      if (RestApiUtils.isRestAPIBased(Some(hc))) {
        val trainFrame = RestApiUtils.getFrame(hc.getConf, trainFrameKey)
        if (trainFrame.columns.find(_.name == getLabelCol()).get.dataType == H2OColumnType.`enum`) {
          RestApiUtils.convertColumnsToCategorical(hc.getConf, trainFrameKey, Array(getLabelCol()))
        }
      } else {
        val trainFrame = DKV.getGet[Frame](trainFrameKey)
        if (!trainFrame.vec(getLabelCol()).isCategorical) {
          trainFrame.replace(trainFrame.find(getLabelCol()),
            trainFrame.vec(getLabelCol()).toCategoricalVec).remove()
        }
      }

    }
  }

  override def fit(dataset: Dataset[_]): H2OSupervisedMOJOModel = {
    super.fit(dataset).asInstanceOf[H2OSupervisedMOJOModel]
  }
}
