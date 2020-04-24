/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.matrix.android.internal.session.group

import androidx.work.ExistingWorkPolicy
import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.session.room.model.Membership
import im.vector.matrix.android.internal.database.RealmLiveEntityObserver
import im.vector.matrix.android.internal.database.awaitTransaction
import im.vector.matrix.android.internal.database.model.GroupEntity
import im.vector.matrix.android.internal.database.model.GroupSummaryEntity
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.di.SessionId
import im.vector.matrix.android.internal.di.WorkManagerProvider
import im.vector.matrix.android.internal.util.MatrixCoroutineDispatchers
import im.vector.matrix.android.internal.worker.WorkerParamsFactory
import io.realm.OrderedCollectionChangeSet
import io.realm.RealmResults
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val GET_GROUP_DATA_WORKER = "GET_GROUP_DATA_WORKER"

internal class GroupSummaryUpdater @Inject constructor(
        private val workManagerProvider: WorkManagerProvider,
        @SessionId private val sessionId: String,
        private val coroutineDispatchers: MatrixCoroutineDispatchers,
        private val monarchy: Monarchy)
    : RealmLiveEntityObserver<GroupEntity>(monarchy.realmConfiguration) {

    override val query = Monarchy.Query { GroupEntity.where(it) }

    override fun onChange(results: RealmResults<GroupEntity>, changeSet: OrderedCollectionChangeSet) {
        // `insertions` for new groups and `changes` to handle left groups
        val modifiedGroupEntity = (changeSet.insertions + changeSet.changes)
                .asSequence()
                .mapNotNull { results[it] }

        fetchGroupsData(modifiedGroupEntity
                .filter { it.membership == Membership.JOIN || it.membership == Membership.INVITE }
                .map { it.groupId }
                .toList())

        modifiedGroupEntity
                .filter { it.membership == Membership.LEAVE }
                .map { it.groupId }
                .toList()
                .also {
                    observerScope.launch {
                        deleteGroups(it)
                    }
                }
    }

    private fun fetchGroupsData(groupIds: List<String>) {
        val getGroupDataWorkerParams = GetGroupDataWorker.Params(sessionId, groupIds)

        val workData = WorkerParamsFactory.toData(getGroupDataWorkerParams)

        val getGroupWork = workManagerProvider.matrixOneTimeWorkRequestBuilder<GetGroupDataWorker>()
                .setInputData(workData)
                .setConstraints(WorkManagerProvider.workConstraints)
                .build()

        workManagerProvider.workManager
                .beginUniqueWork(GET_GROUP_DATA_WORKER, ExistingWorkPolicy.APPEND, getGroupWork)
                .enqueue()
    }

    /**
     * Delete the GroupSummaryEntity of left groups
     */
    private suspend fun deleteGroups(groupIds: List<String>) = awaitTransaction(coroutineDispatchers, monarchy.realmConfiguration) { realm ->
        GroupSummaryEntity.where(realm, groupIds)
                .findAll()
                .deleteAllFromRealm()
    }
}
