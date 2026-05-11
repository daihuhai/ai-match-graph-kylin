<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessageBox } from 'element-plus'
import { useAdminDataStore, type SkillRow, type SynonymRow } from '@/stores/adminData'

const store = useAdminDataStore()

onMounted(() => {
  store.hydrate()
})

const skillDlgOpen = ref(false)
const skillForm = reactive<SkillRow>({ id: '', name: '', category: '' })

const synonymDlgOpen = ref(false)
const synonymForm = reactive<{ id: string; term: string; synonymsText: string }>({
  id: '',
  term: '',
  synonymsText: '',
})

const resetSkillForm = () => {
  skillForm.id = ''
  skillForm.name = ''
  skillForm.category = ''
}

const openSkillCreate = () => {
  resetSkillForm()
  skillDlgOpen.value = true
}

const openSkillEdit = (row: SkillRow) => {
  skillForm.id = row.id
  skillForm.name = row.name
  skillForm.category = row.category
  skillDlgOpen.value = true
}

const saveSkill = () => {
  const id = skillForm.id || `sk-${Date.now()}`
  store.upsertSkill({ id, name: skillForm.name.trim(), category: skillForm.category.trim() })
  skillDlgOpen.value = false
}

const removeSkill = async (row: SkillRow) => {
  await ElMessageBox.confirm(`确认删除技能：${row.name}？`, '提示', { type: 'warning' })
  store.removeSkill(row.id)
}

const resetSynonymForm = () => {
  synonymForm.id = ''
  synonymForm.term = ''
  synonymForm.synonymsText = ''
}

const openSynonymCreate = () => {
  resetSynonymForm()
  synonymDlgOpen.value = true
}

const openSynonymEdit = (row: SynonymRow) => {
  synonymForm.id = row.id
  synonymForm.term = row.term
  synonymForm.synonymsText = row.synonyms.join(', ')
  synonymDlgOpen.value = true
}

const saveSynonym = () => {
  const id = synonymForm.id || `sy-${Date.now()}`
  const synonyms = synonymForm.synonymsText
    .split(',')
    .map((s) => s.trim())
    .filter(Boolean)
  store.upsertSynonym({ id, term: synonymForm.term.trim(), synonyms })
  synonymDlgOpen.value = false
}

const removeSynonym = async (row: SynonymRow) => {
  await ElMessageBox.confirm(`确认删除同义词：${row.term}？`, '提示', { type: 'warning' })
  store.removeSynonym(row.id)
}

const skillOk = computed(() => Boolean(skillForm.name.trim()) && Boolean(skillForm.category.trim()))
const synonymOk = computed(() => Boolean(synonymForm.term.trim()) && Boolean(synonymForm.synonymsText.trim()))
</script>

<template>
  <div class="space-y-4">
    <el-card shadow="never">
      <div class="flex flex-wrap items-start justify-between gap-3">
        <div>
          <div class="text-base font-semibold">数据维护</div>
          <div class="mt-1 text-sm text-zinc-600">技能库与同义词的本地维护（sessionStorage 持久化）。</div>
        </div>
      </div>
    </el-card>

    <el-tabs>
      <el-tab-pane label="技能库">
        <el-card shadow="never">
          <div class="flex items-center justify-between gap-3">
            <div class="text-sm text-zinc-600">用于能力图谱/匹配解释中的标准技能字典。</div>
            <el-button v-permission="'ADMIN_DATA_VIEW'" type="primary" @click="openSkillCreate">新增技能</el-button>
          </div>
          <div class="mt-4">
            <el-table :data="store.skills">
              <el-table-column prop="name" label="技能" min-width="220" />
              <el-table-column prop="category" label="分类" width="160" />
              <el-table-column label="操作" width="180" fixed="right">
                <template #default="{ row }">
                  <el-button v-permission="'ADMIN_DATA_VIEW'" link type="primary" @click="openSkillEdit(row)">编辑</el-button>
                  <el-button v-permission="'ADMIN_DATA_VIEW'" link type="danger" @click="removeSkill(row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="同义词">
        <el-card shadow="never">
          <div class="flex items-center justify-between gap-3">
            <div class="text-sm text-zinc-600">用于解析与匹配阶段的词汇归一（示例）。</div>
            <el-button v-permission="'ADMIN_DATA_VIEW'" type="primary" @click="openSynonymCreate">新增同义词</el-button>
          </div>
          <div class="mt-4">
            <el-table :data="store.synonyms">
              <el-table-column prop="term" label="标准词" width="200" />
              <el-table-column label="同义词" min-width="300">
                <template #default="{ row }">
                  <div class="flex flex-wrap gap-2">
                    <el-tag v-for="s in row.synonyms" :key="s" type="info">{{ s }}</el-tag>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="180" fixed="right">
                <template #default="{ row }">
                  <el-button v-permission="'ADMIN_DATA_VIEW'" link type="primary" @click="openSynonymEdit(row)">编辑</el-button>
                  <el-button v-permission="'ADMIN_DATA_VIEW'" link type="danger" @click="removeSynonym(row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="skillDlgOpen" title="技能" width="460px">
      <el-form label-position="top">
        <el-form-item label="技能名称">
          <el-input v-model="skillForm.name" />
        </el-form-item>
        <el-form-item label="分类">
          <el-input v-model="skillForm.category" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="skillDlgOpen = false">取消</el-button>
        <el-button type="primary" :disabled="!skillOk" @click="saveSkill">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="synonymDlgOpen" title="同义词" width="560px">
      <el-form label-position="top">
        <el-form-item label="标准词">
          <el-input v-model="synonymForm.term" />
        </el-form-item>
        <el-form-item label="同义词（逗号分隔）">
          <el-input v-model="synonymForm.synonymsText" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="synonymDlgOpen = false">取消</el-button>
        <el-button type="primary" :disabled="!synonymOk" @click="saveSynonym">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

