<template>
<div class="page">
  <div class="header">
    <div>
      <div class="title">个性化推荐</div>
      <div class="desc">优先使用协同过滤，不足时自动补热门景点。</div>
    </div>
    <div>
      <el-button @click="$router.push('/index/lvyoushuju')">返回检索</el-button>
      <el-button type="success" @click="exportData">导出推荐结果</el-button>
    </div>
  </div>
  <div class="grid">
    <div class="card" v-for="item in list" :key="item.id" @click="goDetail(item)">
      <img v-if="item.fengmiantupian && item.fengmiantupian.substr(0,4)=='http'" :src="item.fengmiantupian.split(',')[0]" />
      <img v-else :src="baseUrl + (item.fengmiantupian ? item.fengmiantupian.split(',')[0] : '')" />
      <div class="body">
        <div class="name">{{ item.jingdianming }}</div>
        <div class="meta">{{ item.diming }}<span v-if="item.jingdianleixing"> / {{ item.jingdianleixing }}</span></div>
        <div class="meta">热度 {{ item.redu || 0 }} | 评论 {{ item.pinglunrenshu || 0 }}</div>
        <div class="reason">{{ item.recommendReason || '热门兜底推荐' }}</div>
      </div>
    </div>
  </div>
  <el-empty v-if="!list.length" description="暂无推荐结果"></el-empty>
</div>
</template>

<script>
export default {
  data() {
    return { baseUrl: '', list: [] }
  },
  created() {
    this.baseUrl = this.$config.baseUrl
    this.$http.get('lvyoushuju/recommend', { params: { page: 1, limit: 24 } }).then(res => {
      if (res.data.code === 0) this.list = res.data.data.list || []
    })
  },
  methods: {
    goDetail(item) {
      this.$router.push({ path: '/index/lvyoushujuDetail', query: { detailObj: JSON.stringify(item) } })
    },
    exportData() {
      window.open(`${this.baseUrl}lvyoushuju/exportData?format=csv&mode=recommend`)
    }
  }
}
</script>

<style scoped lang="scss">
.page { width: 1200px; margin: 20px auto; }
.header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.title { font-size: 28px; font-weight: 700; color: #2f6f42; }
.desc { margin-top: 8px; color: #728074; }
.grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 18px; }
.card { overflow: hidden; cursor: pointer; background: #fff; border-radius: 14px; box-shadow: 0 8px 24px rgba(33, 72, 50, 0.08); }
.card img { width: 100%; height: 220px; object-fit: cover; }
.body { padding: 14px; }
.name { font-size: 18px; font-weight: 700; color: #22342c; }
.meta { margin-top: 8px; color: #627066; }
.reason { margin-top: 10px; color: #1f7d4d; line-height: 1.6; font-size: 13px; }
</style>
