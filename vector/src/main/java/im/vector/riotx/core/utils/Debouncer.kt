/*

  * Copyright 2019 New Vector Ltd
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
package im.vector.riotx.core.utils

import android.os.Handler

internal class Debouncer(private val handler: Handler) {

    private val runnables = HashMap<String, Runnable>()

    fun debounce(identifier: String, millis: Long, r: Runnable): Boolean {
        if (runnables.containsKey(identifier)) {
            // debounce
            val old = runnables[identifier]
            handler.removeCallbacks(old)
        }
        insertRunnable(identifier, r, millis)
        return true
    }

    fun cancelAll() {
        handler.removeCallbacksAndMessages(null)
    }

    private fun insertRunnable(identifier: String, r: Runnable, millis: Long) {
        val chained = Runnable {
            handler.post(r)
            runnables.remove(identifier)
        }
        runnables[identifier] = chained
        handler.postDelayed(chained, millis)
    }
}