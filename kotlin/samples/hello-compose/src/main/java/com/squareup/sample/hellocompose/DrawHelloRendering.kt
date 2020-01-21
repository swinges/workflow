/*
 * Copyright 2019 Square Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.sample.hellocompose

import androidx.compose.Composable
import androidx.ui.core.Text
import androidx.ui.foundation.Clickable
import androidx.ui.foundation.shape.DrawShape
import androidx.ui.foundation.shape.RectangleShape
import androidx.ui.graphics.Color
import androidx.ui.layout.Center
import androidx.ui.material.MaterialTheme
import androidx.ui.material.ripple.Ripple
import androidx.ui.tooling.preview.Preview
import com.squareup.sample.hellocompose.HelloWorkflow.Rendering

@Composable
fun DrawHelloRendering(rendering: Rendering) {
  Ripple(bounded = true) {
    Clickable(onClick = rendering.onClick) {
      Center {
        // TODO themeTextStyle seems broken in dev03
        Text(rendering.message/*, style = +themeTextStyle { h1 }*/)
      }
    }
  }
}

@Preview(heightDp = 150)
@Composable
fun DrawHelloRenderingPreview() {
  MaterialTheme {
    DrawShape(shape = RectangleShape, color = Color.White)
    DrawHelloRendering(Rendering("Hello!", onClick = {}))
  }
}
