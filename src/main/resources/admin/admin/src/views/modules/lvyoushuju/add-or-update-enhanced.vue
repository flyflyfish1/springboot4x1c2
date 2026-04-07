<template>
  <div class="addEdit-block">
    <el-form ref="ruleForm" :model="ruleForm" :rules="rules" label-width="120px">
      <el-form-item label="城市" prop="diming"><el-input v-model="ruleForm.diming" :readonly="type==='info'"></el-input></el-form-item>
      <el-form-item label="景点名" prop="jingdianming"><el-input v-model="ruleForm.jingdianming" :readonly="type==='info'"></el-input></el-form-item>
      <el-form-item label="景点类型" prop="jingdianleixing"><el-input v-model="ruleForm.jingdianleixing" :readonly="type==='info'"></el-input></el-form-item>
      <el-form-item label="评论人数" prop="pinglunrenshu"><el-input-number v-model="ruleForm.pinglunrenshu" :disabled="type==='info'" :min="0"></el-input-number></el-form-item>
      <el-form-item label="攻略数量" prop="gonglveshuliang"><el-input-number v-model="ruleForm.gonglveshuliang" :disabled="type==='info'" :min="0"></el-input-number></el-form-item>
      <el-form-item label="排名" prop="paiming"><el-input-number v-model="ruleForm.paiming" :disabled="type==='info'" :min="0"></el-input-number></el-form-item>
      <el-form-item label="热度" prop="redu"><el-input-number v-model="ruleForm.redu" :disabled="type==='info'" :min="0"></el-input-number></el-form-item>
      <el-form-item label="宜居性" prop="yijuxing"><el-input-number v-model="ruleForm.yijuxing" :disabled="type==='info'" :min="0"></el-input-number></el-form-item>
      <el-form-item label="星级" prop="xingji"><el-input v-model="ruleForm.xingji" :readonly="type==='info'"></el-input></el-form-item>
      <el-form-item label="经度" prop="jingdu"><el-input v-model="ruleForm.jingdu" :readonly="type==='info'"></el-input></el-form-item>
      <el-form-item label="纬度" prop="weidu"><el-input v-model="ruleForm.weidu" :readonly="type==='info'"></el-input></el-form-item>
      <el-form-item label="封面图" prop="fengmiantupian" v-if="type!=='info'">
        <file-upload action="file/upload" :fileUrls="ruleForm.fengmiantupian" @change="val => ruleForm.fengmiantupian = val"></file-upload>
      </el-form-item>
      <el-form-item label="封面图" v-else>
        <img v-for="(item,index) in (ruleForm.fengmiantupian || '').split(',')" :key="index" :src="$base.url + item" width="100" height="100" />
      </el-form-item>
      <el-form-item label="简介" prop="jianjie">
        <editor v-if="type!=='info'" v-model="ruleForm.jianjie" action="file/upload"></editor>
        <div v-else v-html="ruleForm.jianjie"></div>
      </el-form-item>
      <el-form-item>
        <el-button v-if="type!=='info'" type="primary" @click="onSubmit">提交</el-button>
        <el-button @click="back">{{ type==='info' ? '返回' : '取消' }}</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script>
export default {
  props: ['parent'],
  data() {
    return {
      id: '',
      type: 'edit',
      ruleForm: { diming: '', jingdianming: '', jingdianleixing: '', pinglunrenshu: 0, gonglveshuliang: 0, paiming: 0, redu: 0, yijuxing: 0, xingji: '', jingdu: '', weidu: '', fengmiantupian: '', jianjie: '' },
      rules: { diming: [{ required: true, message: '请输入城市', trigger: 'blur' }], jingdianming: [{ required: true, message: '请输入景点名', trigger: 'blur' }] }
    }
  },
  methods: {
    init(id, type) {
      this.id = id || ''
      this.type = type || 'edit'
      if (id) {
        this.$http({ url: `lvyoushuju/info/${id}`, method: 'get' }).then(({ data }) => { if (data && data.code === 0) this.ruleForm = data.data })
      }
    },
    onSubmit() {
      this.$refs.ruleForm.validate(valid => {
        if (!valid) return
        this.$http({ url: `lvyoushuju/${!this.ruleForm.id ? 'save' : 'update'}`, method: 'post', data: this.ruleForm }).then(({ data }) => {
          if (data && data.code === 0) {
            this.parent.addOrUpdateFlag = false
            this.parent.showFlag = true
            this.parent.getDataList()
          }
        })
      })
    },
    back() {
      this.parent.addOrUpdateFlag = false
      this.parent.showFlag = true
    }
  }
}
</script>
