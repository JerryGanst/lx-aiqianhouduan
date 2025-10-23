<template>
  <Teleport to="body">
    <transition name="term-library-modal-fade">
      <div v-if="visible" class="term-library-modal-overlay" @click="handleOverlayClick">
        <div class="term-library-modal" @click.stop>
          <div class="library_top">
            <div class="head_title">术语库</div>
            <div
              class="close_button"
              role="button"
              tabindex="0"
              @click="closeModal"
              @keydown.enter="closeModal"
              @mouseenter="showCloseTip = true"
              @mouseleave="showCloseTip = false"
              aria-label="关闭"
            >
              <img src="@/assets/knowledgeBase/close.png" alt="关闭" />
              <transition name="fade">
                <div v-if="showCloseTip" class="tooltip-bottom">关闭</div>
              </transition>
            </div>
          </div>
          <div class="library_content">
            <div class="library_operation">
              <div class="library_operation_buttons">
                <div class="left_batch_delete btn" @click="handleBatchDelete">删除</div>
                <div class="enable_operation btn" @click="handleBatchEnable">生效</div>
                <div class="disable_operation btn" @click="handleBatchDisable">失效</div>
              </div>
              <div class="right_operations">
                <div class="add_library" @click="openAddTermModal">添加术语</div>
                <div class="search_library">
                  <el-input
                    v-model="searchKeyword"
                    placeholder="搜索术语"
                    @keyup.enter="handleSearch"
                  >
                    <template #prefix>
                      <el-icon class="el-input__icon">
                        <Search />
                      </el-icon>
                    </template>
                  </el-input>
                </div>
              </div>
            </div>
            <div class="library_title library-row">
              <div class="library-row__checkbox">
                <MenuCheckBox
                  :show="true"
                  menu-id="all"
                  :checked="isAllChecked"
                  @update-selected="handleCheckboxUpdate"
                />
              </div>
              <span class="library-row__zh">中文</span>
              <span class="library-row__en">英文</span>
              <span class="library-row__es">西班牙语</span>
              <span class="library-row__vi">越南语</span>
              <span class="library-row__operation normal_handle">操作</span>
              <span class="library-row__operation status">状态</span>
            </div>
            <div class="library_list">
              <div
                v-for="(item, index) in libraryList"
                :key="`${item.glossary_id}-${index}`"
                class="library-row library-item"
              >
                <div class="library-row__checkbox">
                  <MenuCheckBox
                    :show="true"
                    :menu-id="item.glossary_id"
                    :checked="checkedIdList.includes(item.glossary_id)"
                    @update-selected="handleCheckboxUpdate"
                  />
                </div>
                <span
                  class="library-row__zh"
                  :title="item.zh"
                >
                  {{ item.zh }}
                </span>
                <span
                  class="library-row__en"
                  :title="item.en"
                >
                  {{ item.en }}
                </span>
                <span
                  class="library-row__es"
                  :title="item.es"
                >
                  {{ item.es }}
                </span>
                <span
                  class="library-row__vi"
                  :title="item.vi"
                >
                  {{ item.vi }}
                </span>
                <div class="library-row__operation list_operation">
                  <span class="operation-edit" @click="openEditTermModal(item)">编辑</span>
                  <span
                    class="operation-delete"
                    @click="deleteTerm(item.glossary_id)"
                  >
                    删除
                  </span>
                  <span
                    class="operation-toggle"
                    :class="statusClassMap[item.status]"
                    @click="toggleStatus(item.glossary_id)"
                  >
                    {{ getStatusLabel(item.status) }}
                  </span>
                </div>
              </div>
            </div>
          </div>
          <div class="library_buttons">
            <button
              class="library_button library_button--cancel"
              type="button"
              @click="closeModal"
            >
              取消
            </button>
            <button
              class="library_button library_button--confirm"
              type="button"
              @click="handleConfirm"
            >
              确认
            </button>
          </div>
        </div>
      </div>
    </transition>
    <AddTermModal
      v-model:visible="showAddTermModal"
      :term="editingTerm"
      @close="closeAddTermModal"
      @save="handleTermSave"
    />
  </Teleport>
</template>

<script setup>
import { computed, ref, toRefs, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import MenuCheckBox from '../options/menuCheckBox.vue'
import AddTermModal from './AddTermModal.vue'
import { useShared } from '@/utils/useShared'
import { queryGlossaryItems, updateGlossaryItems } from '@/api/glossary/actions'

const generateGlossaryId = () => {
  const globalCrypto = globalThis?.crypto

  if (globalCrypto?.randomUUID) {
    return globalCrypto.randomUUID().replace(/-/g, '')
  }

  const alphabet =
    '0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ_abcdefghijklmnopqrstuvwxyz-'
  const alphabetLength = alphabet.length
  const idLength = 21

  let id = ''

  for (let index = 0; index < idLength; index += 1) {
    const randomIndex = Math.floor(Math.random() * alphabetLength)
    id += alphabet[randomIndex]
  }

  return id
}

const TermStatus = Object.freeze({
  ACTIVE: 'Active',
  EXPIRED: 'Expired',
  PENDING: 'Pending',
  DEACTIVATED: 'Deactivated'
})

const statusLabels = {
  [TermStatus.ACTIVE]: '已生效',
  [TermStatus.EXPIRED]: '已失效',
  [TermStatus.PENDING]: '待生效',
  [TermStatus.DEACTIVATED]: '待失效'
}

const statusClassMap = {
  [TermStatus.ACTIVE]: 'operation-toggle--enable',
  [TermStatus.EXPIRED]: 'operation-toggle--disable',
  [TermStatus.PENDING]: 'operation-toggle--pending',
  [TermStatus.DEACTIVATED]: 'operation-toggle--deactivated'
}

const showCloseTip = ref(false)
const searchKeyword = ref('')
const showAddTermModal = ref(false)
const editingTerm = ref(null)
const { userInfo } = useShared()

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  closeOnClickOverlay: {
    type: Boolean,
    default: true
  }
})

// 点击保存时保存的操作数组
const operationList = ref([])

const checkedIdList = ref([])

const isAllChecked = computed(
  () =>
    libraryList.value.length > 0 &&
    checkedIdList.value.length === libraryList.value.length
)

const libraryList = ref([])

const normalizeTermItem = item => ({
  ...(item || {}),
  status: item?.enabled ? TermStatus.ACTIVE : TermStatus.EXPIRED
})

const resetTermLibraryState = () => {
  libraryList.value = []
  operationList.value = []
  checkedIdList.value = []
}

const resolveEnabledValue = operationItem => {
  if (typeof operationItem?.enabled === 'boolean') {
    return operationItem.enabled
  }

  const status = operationItem?.status

  if (status === TermStatus.ACTIVE || status === TermStatus.PENDING) {
    return true
  }

  if (status === TermStatus.EXPIRED || status === TermStatus.DEACTIVATED) {
    return false
  }

  const libraryItem = libraryList.value.find(
    item => item.glossary_id === operationItem?.glossary_id
  )

  if (libraryItem) {
    if (typeof libraryItem.enabled === 'boolean') {
      return libraryItem.enabled
    }

    if (
      libraryItem.status === TermStatus.ACTIVE ||
      libraryItem.status === TermStatus.PENDING
    ) {
      return true
    }

    if (
      libraryItem.status === TermStatus.EXPIRED ||
      libraryItem.status === TermStatus.DEACTIVATED
    ) {
      return false
    }
  }

  return true
}

const extractErrorMessage = error => {
  const responseData = error?.response?.data

  if (typeof responseData === 'string' && responseData.trim()) {
    return responseData
  }

  if (responseData && typeof responseData === 'object') {
    const message =
      responseData.message || responseData.msg || responseData.error || ''

    if (message) {
      return message
    }
  }

  if (typeof error?.message === 'string' && error.message.trim()) {
    return error.message
  }

  return '术语库更新失败'
}

const loadTermLibrary = async ({ query, resetKeyword = false } = {}) => {
  const userId = userInfo.value?.id

  if (resetKeyword) {
    searchKeyword.value = ''
  }


  const normalizedQuery =
    typeof query === 'string'
      ? query.trim()
      : resetKeyword
        ? ''
        : searchKeyword.value.trim()

  if (!resetKeyword && typeof query === 'string') {
    searchKeyword.value = normalizedQuery
  }

  if (!userId) {
    resetTermLibraryState()
    return
  }

  const requestPayload = {
    user_id: userId,
    scope: 'user'
  }

  if (normalizedQuery) {
    requestPayload.query = normalizedQuery
    requestPayload.threshold = 80
  }

  try {
    const response = await queryGlossaryItems(requestPayload)
    const responseData = response?.data ?? response ?? {}
    const items = Array.isArray(responseData.items) ? responseData.items : []

    libraryList.value = items.map(normalizeTermItem)
    operationList.value = []
    checkedIdList.value = []
  } catch (error) {
    console.error('Failed to load term library list:', error)
    resetTermLibraryState()
    ElMessage.error('术语列表加载失败')
  }
}

const initializeTermLibrary = () => loadTermLibrary({ resetKeyword: true })

const handleSearch = async () => {
  await loadTermLibrary({ query: searchKeyword.value })
}

const getStatusLabel = status => statusLabels[status] || status

const toggleStatus = glossaryId => {
  const item = libraryList.value.find(
    libraryItem => libraryItem.glossary_id === glossaryId
  )

  if (!item) {
    return
  }

  if (item.status === TermStatus.ACTIVE) {
    item.status = TermStatus.EXPIRED
    return
  }

  if (item.status === TermStatus.EXPIRED) {
    item.status = TermStatus.ACTIVE
    return
  }

  item.status = TermStatus.PENDING
}

const deleteTerm = (glossaryId, shouldNotify = true) => {
  const libraryIndex = libraryList.value.findIndex(
    item => item.glossary_id === glossaryId
  )

  if (libraryIndex === -1) {
    return
  }

  const [removedItem] = libraryList.value.splice(libraryIndex, 1)

  if (!removedItem) {
    return
  }

  checkedIdList.value = checkedIdList.value.filter(
    id => id !== removedItem.glossary_id
  )

  const timestamp = new Date().toISOString()
  const userId = userInfo.value?.id || removedItem.user_id

  const operationIndex = operationList.value.findIndex(
    item => item.glossary_id === removedItem.glossary_id
  )

  if (operationIndex !== -1) {
    const existingOperation = operationList.value[operationIndex]

    operationList.value.splice(operationIndex, 1, {
      ...existingOperation,
      operation_type: 'delete',
      update_at: timestamp,
      updated_by: userId || existingOperation.updated_by
    })

    return
  }

  operationList.value.unshift({
    ...removedItem,
    operation_type: 'delete',
    update_at: timestamp,
    updated_by: userId
  })

  if (shouldNotify) {
    ElMessage.success('标记删除成功')
  }
}

const handleCheckboxUpdate = (menuId, isSelected) => {
  if (menuId === 'all') {
    checkedIdList.value = isSelected
      ? libraryList.value.map(item => item.glossary_id)
      : []
    return
  }

  if (!menuId) {
    return
  }

  if (isSelected) {
    if (!checkedIdList.value.includes(menuId)) {
      checkedIdList.value.push(menuId)
    }
    return
  }

  checkedIdList.value = checkedIdList.value.filter(id => id !== menuId)
}

const handleBatchDelete = () => {
  if (!checkedIdList.value.length) {
    ElMessage.warning('请勾选您需要删除的术语')
    return
  }

  const idsToDelete = [...checkedIdList.value]

  idsToDelete.forEach(glossaryId => {
    deleteTerm(glossaryId, false)
  })

  ElMessage.success('标记删除成功')
}

const handleBatchEnable = () => {
  if (!checkedIdList.value.length) {
    ElMessage.warning('请勾选您需要生效的术语')
    return
  }

  const checkedIdSet = new Set(checkedIdList.value)
  const pendingInsertIdSet = new Set(checkedIdList.value)

  libraryList.value.forEach(item => {
    if (checkedIdSet.has(item.glossary_id)) {
      item.status = TermStatus.PENDING
    }
  })

  operationList.value.forEach(operation => {
    if (checkedIdSet.has(operation.glossary_id)) {
      operation.enabled = true
      if (!operation.operation_type) {
        operation.operation_type = 'edit'
      }
      pendingInsertIdSet.delete(operation.glossary_id)
    }
  })

  if (pendingInsertIdSet.size) {
    const timestamp = new Date().toISOString()
    const userId = userInfo.value?.id || ''

    Array.from(pendingInsertIdSet).forEach(glossaryId => {
      const libraryItem = libraryList.value.find(
        item => item.glossary_id === glossaryId
      )

      if (!libraryItem) {
        return
      }

      operationList.value.unshift({
        ...libraryItem,
        enabled: true,
        updated_by: userId || libraryItem.updated_by,
        update_at: timestamp,
        operation_type: 'edit'
      })
    })
  }

  ElMessage.success('标记生效成功')
}

const handleBatchDisable = () => {
  if (!checkedIdList.value.length) {
    ElMessage.warning('请勾选您需要失效的术语')
    return
  }

  const checkedIdSet = new Set(checkedIdList.value)
  const pendingInsertIdSet = new Set(checkedIdList.value)

  libraryList.value.forEach(item => {
    if (checkedIdSet.has(item.glossary_id)) {
      item.status = TermStatus.DEACTIVATED
    }
  })

  operationList.value.forEach(operation => {
    if (checkedIdSet.has(operation.glossary_id)) {
      operation.enabled = false
      if (!operation.operation_type) {
        operation.operation_type = 'edit'
      }
      pendingInsertIdSet.delete(operation.glossary_id)
    }
  })

  if (pendingInsertIdSet.size) {
    const timestamp = new Date().toISOString()
    const userId = userInfo.value?.id || ''

    Array.from(pendingInsertIdSet).forEach(glossaryId => {
      const libraryItem = libraryList.value.find(
        item => item.glossary_id === glossaryId
      )

      if (!libraryItem) {
        return
      }

      operationList.value.unshift({
        ...libraryItem,
        enabled: false,
        updated_by: userId || libraryItem.updated_by,
        update_at: timestamp,
        operation_type: 'edit'
      })
    })
  }

  ElMessage.success('标记失效成功')
}

const handleConfirm = async () => {
  const userId = userInfo.value?.id || ''
  const operations = operationList.value
    .filter(
      item =>
        item &&
        typeof item.glossary_id === 'string' &&
        item.glossary_id &&
        typeof item.operation_type === 'string' &&
        item.operation_type
    )
    .map(item => ({
      glossary_id: item.glossary_id,
      en: item.en ?? null,
      zh: item.zh ?? null,
      vi: item.vi ?? null,
      es: item.es ?? null,
      enabled: resolveEnabledValue(item),
      operation_type: item.operation_type
    }))

  closeModal()

  if (!userId) {
    ElMessage.error('无法获取用户信息，术语库未更新')
    return
  }

  if (!operations.length) {
    ElMessage.info('暂无需要更新的术语')
    return
  }

  try {
    await updateGlossaryItems({
      user_id: userId,
      actor_id: userId,
      items: operations
    })

    ElMessage.success('术语库更新成功')
  } catch (error) {
    const message = extractErrorMessage(error)
    ElMessage.error(message)
  }
}

const emit = defineEmits(['update:visible'])

const { visible } = toRefs(props)

const handleOverlayClick = () => {
  if (props.closeOnClickOverlay) {
    emit('update:visible', false)
  }
}

const closeModal = () => {
  emit('update:visible', false)
}

const openAddTermModal = () => {
  editingTerm.value = null
  showAddTermModal.value = true
}

const closeAddTermModal = () => {
  editingTerm.value = null
  showAddTermModal.value = false
}

const openEditTermModal = term => {
  if (!term) {
    return
  }

  editingTerm.value = { ...term }
  showAddTermModal.value = true
}

const handleTermSave = term => {
  const userId = userInfo.value?.id || ''
  const timestamp = new Date().toISOString()

  if (editingTerm.value) {
    const glossaryId = editingTerm.value.glossary_id
    const libraryIndex = libraryList.value.findIndex(
      item => item.glossary_id === glossaryId
    )

    if (libraryIndex !== -1) {
      const currentLibraryItem = libraryList.value[libraryIndex]
      const updatedLibraryItem = {
        ...currentLibraryItem,
        zh: term.zh,
        en: term.en,
        es: term.es,
        vi: term.vi,
        status: TermStatus.PENDING,
        update_at: timestamp,
        updated_by: userId || currentLibraryItem.updated_by
      }

      libraryList.value.splice(libraryIndex, 1, updatedLibraryItem)
    }

    const operationIndex = operationList.value.findIndex(
      item => item.glossary_id === glossaryId
    )

    const baseOperationItem =
      operationIndex !== -1
        ? operationList.value[operationIndex]
        : editingTerm.value

    const updatedOperationItem = {
      ...baseOperationItem,
      glossary_id: glossaryId,
      user_id: baseOperationItem?.user_id || userId,
      zh: term.zh,
      en: term.en,
      es: term.es,
      vi: term.vi,
      enabled: true,
      updated_by: userId || baseOperationItem?.updated_by,
      create_at: baseOperationItem?.create_at || editingTerm.value.create_at,
      update_at: timestamp,
      operation_type: 'edit'
    }

    if (operationIndex !== -1) {
      operationList.value.splice(operationIndex, 1, updatedOperationItem)
    } else {
      operationList.value.unshift(updatedOperationItem)
    }

    closeAddTermModal()

    return
  }

  const newGlossaryId = generateGlossaryId()
  const newTerm = {
    glossary_id: newGlossaryId,
    user_id: userId,
    zh: term.zh,
    en: term.en,
    es: term.es,
    vi: term.vi,
    enabled: true,
    updated_by: userId,
    create_at: timestamp,
    update_at: timestamp
  }

  operationList.value.unshift({
    ...newTerm,
    operation_type: 'add'
  })
  libraryList.value.unshift({
    ...newTerm,
    status: TermStatus.PENDING
  })

  closeAddTermModal()
}

watch(
  () => props.visible,
  value => {
    if (value) {
      initializeTermLibrary()
    }
  }
)

watch(
  () => libraryList.value,
  newList => {
    const validIds = newList.map(item => item.glossary_id)
    checkedIdList.value = checkedIdList.value.filter(id =>
      validIds.includes(id)
    )
  },
  { deep: true }
)
</script>

<style scoped lang="less">
.library_operation_buttons {
  display: flex;
  .btn {
    margin-right: 10px;
  }
}
.status {
  transform: translateX(8px);
}
.list_operation {
  transform: translateX(10px);
}
.normal_handle {
  transform: translateX(-55px);
}
.library_top {
  height: 73.5px;
  width: 100%;
  display: flex;
  justify-content: space-between;
  border-bottom: 1px solid #ede9e9;
  align-items: center;
  .head_title {
    font-size: 22px;
    font-weight: 500;
    color: #000000;
    margin-left: 24px;
  }
}
.close_button {
  margin-right: 25px;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  transition: background-color 0.2s ease;
  outline: none;
}

.close_button:hover,
.close_button:focus-visible {
  background-color: #eee;
}

.close_button img {
  width: 16px;
  height: 16px;
}

.tooltip-bottom {
  position: absolute;
  top: calc(100% + 5px);
  left: 50%;
  transform: translateX(-50%);
  background: #000;
  color: #fff;
  padding: 6px 12px;
  border-radius: 4px;
  font-size: 14px;
  white-space: nowrap;
  z-index: 100;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.tooltip-bottom::after {
  content: '';
  position: absolute;
  bottom: 100%;
  left: 50%;
  transform: translateX(-50%);
  border: 5px solid transparent;
  border-bottom-color: #000;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
.library_content {
  height: 625.5px;
  width: 100%;
  flex: 1 1 auto;
  display: flex;
  flex-direction: column;
  .library_operation {
    width: 100%;
    height: 64px;
    box-sizing: border-box;
    padding: 16px 24px;
    display: flex;
    justify-content: space-between;
    .left_batch_delete {
      width: 88px;
      height: 32px;
      border-radius: 4px;
      background: #fff;
      border: 1px solid #ff4d4f;
      box-sizing: border-box;
      text-align: center;
      line-height: 30px;
      color: #ff4d4f;
      cursor: pointer;
      font-size: 14px;
    }
    .enable_operation {
      width: 88px;
      height: 32px;
      border-radius: 4px;
      background: #fff;
      border: 1px solid #52C41A;
      box-sizing: border-box;
      text-align: center;
      line-height: 30px;
      color: #52C41A;
      cursor: pointer;
      font-size: 14px;
    }
    .disable_operation {
      width: 88px;
      height: 32px;
      border-radius: 4px;
      background: #fff;
      border: 1px solid #FF9F0A;
      box-sizing: border-box;
      text-align: center;
      line-height: 30px;
      color: #FF9F0A;
      cursor: pointer;
      font-size: 14px;
    }
    .right_operations {
      display: flex;
      align-items: center;
      .add_library {
        width: 88px;
        height: 32px;
        border-radius: 4px;
        background: #1b6cff;
        color: white;
        cursor: pointer;
        font-size: 14px;
        text-align: center;
        line-height: 30px;
        margin-right: 8px;
        box-sizing: border-box;
      }
      .search_library {
        display: flex;
        align-items: center;
        font-size: 14px;
        color: #333333;
        :deep(.el-input) {
          --el-input-height: 35px;
          --el-input-width: 294px;
          --el-input-border-radius: 8px;
          --el-input-border-color: #DCE6FA;
          --el-input-hover-border-color: #DCE6FA;
          --el-input-focus-border-color: #409EFF;
        }
      }
    }
  }
  .library_title {
    width: 1150px;
    height: 36px;
    border-radius: 6px;
    background: #f5f8ff;
    margin-left: 24px;
    font-size: 14px;
    color: #333333;

    .library-row__operation {
      color: #333333;
      font-weight: 500;
    }
    .library-row__en {
      transform: translateX(-23px);
    }
    .library-row__es {
      transform: translateX(-44px);
    }
    .library-row__vi {
      transform: translateX(-62px);
    }
  }

  .library_list {
    margin-top: 8px;
    max-height: 516px;
    overflow-y: auto;
  }
}
.library-row {
  width: 1150px;
  height: 36px;
  display: flex;
  align-items: center;
  box-sizing: border-box;
  padding: 0 42px 0 16px;
  font-size: 14px;
  color: #333333;
}

.library-item {
  margin-left: 24px;
  border-radius: 6px;
}

.library-item + .library-item {
  margin-top: 8px;
}

.library-row__checkbox {
  width: 36px;
  display: flex;
  align-items: center;
  justify-content: flex-start;

  :deep(.check_box) {
    margin-right: 0;
  }
}

.library-row__zh,
.library-row__en,
.library-row__es,
.library-row__vi {
  display: inline-block;
  align-items: center;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.library-row__zh {
  width: 221px;
  margin-right: 24px;
}

.library-row__en {
  width: 233px;
  margin-right: 24px;
}

.library-row__es {
  width: 230px;
  margin-right: 24px;
}

.library-row__vi {
  width: 230px;
}

.library-row__operation {
  display: flex;
  align-items: center;
  margin-left: 6px;
  white-space: nowrap;
}

.operation-edit {
  color: #1B6CFF;
  font-size: 14px;
  cursor: pointer;
  margin-right: 24px;
}

.operation-delete {
  color: #FF4D4F;
  font-size: 14px;
  cursor: pointer;
  margin-right: 24px;
}

.operation-toggle {
  font-size: 14px;
  cursor: pointer;
  transition: color 0.2s ease;
  transform: translateX(10px);
}

.operation-toggle--disable {
  color: #FF9F0A;
}

.operation-toggle--enable {
  color: #52C41A;
}

.operation-toggle--pending {
  color: #1B6CFF;
}

.operation-toggle--deactivated {
  color: #9EA3B4;
}
.library_buttons {
  width: 100%;
  display: flex;
  justify-content: center;
  align-items: flex-end;
  gap: 8px;
  padding: 24px 0 32px;
  box-sizing: border-box;
  margin-top: auto;
}

.library_button {
  width: 274px;
  height: 46px;
  border-radius: 6px;
  border: 1px solid transparent;
  font-size: 14px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background-color 0.2s ease, color 0.2s ease, border-color 0.2s ease;
}

.library_button--cancel {
  background: #ffffff;
  border-color: #dedede;
  color: #000000;
}

.library_button--confirm {
  background: #1b6cff;
  color: #ffffff;
}

.library_button:focus-visible {
  outline: none;
  box-shadow: 0 0 0 2px rgba(27, 108, 255, 0.2);
}

.term-library-modal {
  width: 1199.5px;
  height: 800px;
  background: #fff;
  border-radius: 20px;
  box-shadow: 0 10px 40px rgba(15, 18, 22, 0.12);
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
}
.term-library-modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 3000;
}

:global(.el-message) {
  z-index: 4000 !important;
}

.term-library-modal-fade-enter-active,
.term-library-modal-fade-leave-active {
  transition: opacity 0.2s ease;
}

.term-library-modal-fade-enter-from,
.term-library-modal-fade-leave-to {
  opacity: 0;
}

</style>
