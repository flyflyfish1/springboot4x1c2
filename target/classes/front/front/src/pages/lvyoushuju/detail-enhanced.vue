<template>
<div class="detail-page">
  <div class="hero">
    <div class="gallery">
      <el-carousel height="420px" indicator-position="outside">
        <el-carousel-item v-for="(item,index) in detailBanner" :key="index">
          <img v-if="item && item.substr(0,4)=='http'" :src="item" />
          <img v-else :src="baseUrl + item" />
        </el-carousel-item>
      </el-carousel>
    </div>
    <div class="info">
      <div class="title">{{ detail.jingdianming }}</div>
      <div class="city">{{ detail.diming }}<span v-if="detail.jingdianleixing"> / {{ detail.jingdianleixing }}</span></div>
      <div class="tags" v-if="detail.cityTags">{{ detail.cityTags }}</div>
      <div class="stats">
        <div>排名：{{ detail.paiming || 0 }}</div>
        <div>评论人数：{{ detail.pinglunrenshu || 0 }}</div>
        <div>热度：{{ detail.redu || 0 }}</div>
        <div>宜居性：{{ detail.yijuxing || 0 }}</div>
        <div>点击次数：{{ detail.clicknum || 0 }}</div>
        <div>星级：{{ detail.xingji || '-' }}</div>
      </div>
      <div class="actions">
        <el-button type="warning" v-if="!isStoreup" @click="storeup(1)">收藏</el-button>
        <el-button v-else @click="storeup(-1)">取消收藏</el-button>
        <el-button type="success" @click="markVisited">我去过</el-button>
      </div>
      <div class="city-profile" v-if="cityProfile">
        <div class="profile-title">城市画像</div>
        <div class="profile-tags">{{ cityProfile.biaoqian || detail.cityTags }}</div>
        <div class="profile-text">{{ cityProfile.jianjie }}</div>
      </div>
    </div>
  </div>

  <el-tabs class="tabs" v-model="activeName">
    <el-tab-pane label="景点简介" name="intro">
      <div v-html="detail.jianjie"></div>
    </el-tab-pane>
    <el-tab-pane label="评论" name="comment">
      <el-form :model="form" :rules="rules" ref="form">
        <el-form-item prop="content">
          <el-input type="textarea" :rows="5" v-model="form.content" placeholder="输入评论"></el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="submitForm('form')">提交评论</el-button>
          <el-button @click="resetForm('form')">重置</el-button>
        </el-form-item>
      </el-form>
      <div class="comment-item" v-for="item in infoList" :key="item.id">
        <div class="comment-user">{{ item.nickname }}</div>
        <div class="comment-content">{{ item.content }}</div>
        <div class="comment-reply" v-if="item.reply">回复：{{ item.reply }}</div>
      </div>
    </el-tab-pane>
  </el-tabs>
</div>
</template>

<script>
import CountDown from '@/components/CountDown'
export default {
  components: { CountDown },
  data() {
    return {
      tablename: 'lvyoushuju',
      baseUrl: '',
      detail: {},
      detailBanner: [],
      activeName: 'intro',
      cityProfile: null,
      form: { content: '', userid: localStorage.getItem('userid'), nickname: localStorage.getItem('username'), avatarurl: '' },
      rules: { content: [{ required: true, message: '请输入评论', trigger: 'blur' }] },
      infoList: [],
      isStoreup: false,
      storeupInfo: {}
    }
  },
  created() {
    this.baseUrl = this.$config.baseUrl
    if (this.$route.query.detailObj) this.detail = JSON.parse(this.$route.query.detailObj)
    if (this.$route.query.storeupObj) this.detail.id = JSON.parse(this.$route.query.storeupObj).refid
    this.loadDetail()
    this.getDiscussList()
    this.getStoreupStatus()
  },
  methods: {
    loadDetail() {
      this.$http.get(`${this.tablename}/detail/${this.detail.id}`).then(res => {
        if (res.data.code === 0) {
          this.detail = res.data.data
          this.detailBanner = this.detail.fengmiantupian ? this.detail.fengmiantupian.split(',') : []
          if (localStorage.getItem('Token')) this.$http.get(`lvyoushuju/behavior/${this.detail.id}`, { params: { type: 'browse' } })
          if (this.detail.diming) this.$http.get('cityprofile/byCity', { params: { chengshi: this.detail.diming } }).then(r => { if (r.data.code === 0) this.cityProfile = r.data.data })
        }
      })
    },
    markVisited() {
      this.$http.get(`lvyoushuju/visited/${this.detail.id}`).then(() => this.$message.success('已记录为去过'))
    },
    storeup(type) {
      if (type === 1 && !this.isStoreup) {
        const payload = { name: this.detail.jingdianming, picture: this.detailBanner[0], inteltype: this.detail.diming, refid: this.detail.id, tablename: 'lvyoushuju', type: 1, userid: localStorage.getItem('userid') }
        this.$http.post('storeup/add', payload).then(res => {
          if (res.data.code === 0) {
            this.isStoreup = true
            this.$http.get(`lvyoushuju/behavior/${this.detail.id}`, { params: { type: 'favorite' } })
            this.$message.success('收藏成功')
          }
        })
      } else if (type === -1 && this.isStoreup) {
        this.$http.get('storeup/list', { params: { page: 1, limit: 1, type: 1, refid: this.detail.id, tablename: 'lvyoushuju', userid: localStorage.getItem('userid') } }).then(res => {
          if (res.data.code === 0 && res.data.data.list.length > 0) {
            this.storeupInfo = res.data.data.list[0]
            this.$http.post('storeup/delete', [this.storeupInfo.id]).then(() => {
              this.isStoreup = false
              this.$message.success('已取消收藏')
            })
          }
        })
      }
    },
    getStoreupStatus() {
      if (!localStorage.getItem('Token')) return
      this.$http.get('storeup/list', { params: { page: 1, limit: 1, type: 1, refid: this.detail.id, tablename: 'lvyoushuju', userid: localStorage.getItem('userid') } }).then(res => {
        if (res.data.code === 0 && res.data.data.list.length > 0) this.isStoreup = true
      })
    },
    getDiscussList() {
      this.$http.get('discusslvyoushuju/list', { params: { page: 1, limit: 20, refid: this.detail.id } }).then(res => {
        if (res.data.code === 0) this.infoList = res.data.data.list
      })
    },
    submitForm(formName) {
      this.$refs[formName].validate(valid => {
        if (!valid) return
        this.form.refid = this.detail.id
        this.form.avatarurl = localStorage.getItem('headportrait') || ''
        this.$http.post('discusslvyoushuju/add', this.form).then(res => {
          if (res.data.code === 0) {
            this.form.content = ''
            this.getDiscussList()
            this.$message.success('评论成功')
          }
        })
      })
    },
    resetForm(formName) {
      this.$refs[formName].resetFields()
    }
  }
}
</script>

<style scoped lang="scss">
.detail-page { width: 1200px; margin: 20px auto; }
.hero { display: grid; grid-template-columns: 1.1fr 0.9fr; gap: 24px; }
.gallery, .info, .tabs { background: #fff; border-radius: 16px; box-shadow: 0 8px 24px rgba(33, 72, 50, 0.08); }
.gallery { padding: 18px; }
.gallery img { width: 100%; height: 420px; object-fit: cover; border-radius: 12px; }
.info { padding: 24px; }
.title { font-size: 30px; font-weight: 700; color: #22342c; }
.city { margin-top: 12px; color: #5c6f63; font-size: 16px; }
.tags { margin-top: 12px; color: #1f7d4d; line-height: 1.6; }
.stats { display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px; margin-top: 20px; }
.stats div { padding: 12px; background: #f4faf2; border-radius: 10px; color: #314339; }
.actions { margin-top: 20px; }
.city-profile { margin-top: 24px; padding: 16px; background: #f8fcf6; border-radius: 12px; }
.profile-title { font-weight: 700; color: #22342c; }
.profile-tags { margin-top: 8px; color: #1f7d4d; }
.profile-text { margin-top: 10px; color: #627066; line-height: 1.7; }
.tabs { margin-top: 24px; padding: 20px; }
.comment-item { padding: 14px 0; border-bottom: 1px solid #edf3ea; }
.comment-user { font-weight: 700; color: #22342c; }
.comment-content, .comment-reply { margin-top: 8px; color: #5f6d64; line-height: 1.7; }
</style>
