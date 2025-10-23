// src/utils/directives.js
// 禁用拖拽指令
const disableDrag = {
  mounted(el, { value }) {
    el._dragHandler = e => {
      if (value) {
        e.preventDefault()
        e.stopPropagation()
      }
    }
    // 捕获阶段拦截，确保优先级最高
    document.addEventListener('dragover', el._dragHandler, true)
    document.addEventListener('drop', el._dragHandler, true)
  },
  updated(el, { value }) {
    // 值更新时同步状态
    el._dragHandler = e => {
      if (value) {
        e.preventDefault()
        e.stopPropagation()
      }
    }
  },
  unmounted(el) {
    // 清理监听器
    document.removeEventListener('dragover', el._dragHandler, true)
    document.removeEventListener('drop', el._dragHandler, true)
  }
}

// 全选指令：为 input/textarea 支持 Ctrl/Cmd + A
const selectAllOnCtrlA = {
  mounted(el) {
    const resolveEditable = target => {
      // ElementPlus 的 el-input 组件，真实可编辑元素位于 input 或 textarea
      if (target?.tagName === 'TEXTAREA' || target?.tagName === 'INPUT') return target
      const textarea = target.querySelector('textarea')
      if (textarea) return textarea
      const input = target.querySelector('input')
      if (input) return input
      return null
    }

    el._onKeydownSelectAll = e => {
      if ((e.ctrlKey || e.metaKey) && (e.key === 'a' || e.key === 'A')) {
        const editable = resolveEditable(el)
        if (editable) {
          e.preventDefault()
          editable.select()
        }
      }
    }
    el.addEventListener('keydown', el._onKeydownSelectAll)
  },
  unmounted(el) {
    el.removeEventListener('keydown', el._onKeydownSelectAll)
  }
}

export { disableDrag, selectAllOnCtrlA }
