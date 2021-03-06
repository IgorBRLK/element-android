/*
 * Copyright (c) 2020 New Vector Ltd
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
package im.vector.matrix.android.internal.session.widgets

import im.vector.matrix.android.internal.session.openid.RequestOpenIdTokenResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

internal interface WidgetsAPI {

    /**
     * register to the server
     *
     * @param body the body content (Ref: https://github.com/matrix-org/matrix-doc/pull/1961)
     */
    @POST("register")
    fun register(@Body body: RequestOpenIdTokenResponse,
                 @Query("v") version: String?): Call<RegisterWidgetResponse>

    @GET("account")
    fun validateToken(@Query("scalar_token") scalarToken: String?,
                      @Query("v") version: String?): Call<Unit>
}
