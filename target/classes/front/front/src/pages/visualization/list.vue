<template>
<div class="page">
  <div class="header">
    <div class="title">城市景点可视化</div>
    <el-button @click="$router.push('/index/lvyoushuju')">返回检索</el-button>
  </div>
  <div class="filters">
    <el-select v-model="city" clearable placeholder="全部城市" @change="loadData">
      <el-option v-for="item in cities" :key="item" :label="item" :value="item"></el-option>
    </el-select>
    <el-radio-group v-model="metric" @change="loadData">
      <el-radio-button label="paiming">按排名</el-radio-button>
      <el-radio-button label="pinglunrenshu">按评论人数</el-radio-button>
      <el-radio-button label="redu">按热度</el-radio-button>
    </el-radio-group>
  </div>
  <div class="bars">
    <div class="bar-row" v-for="item in list" :key="item.rank">
      <div class="bar-label">{{ item.rank }}. {{ item.name }}</div>
      <div class="bar-track">
        <div class="bar-fill" :style="{ width: percent(item.value) }"></div>
      </div>
      <div class="bar-value">{{ item.value }}</div>
    </div>
  </div>
</div>
</template>

<script>
export default {
  data() {
    return { city: '', metric: 'paiming', list: [], cities: [], maxValue: 1 }
  },
  created() {
    this.$http.get('lvyoushuju/rankings').then(res => {
      if (res.data.code === 0) this.cities = res.data.data.cities || []
    })
    this.loadData()
  },
  methods: {
    loadData() {
      this.$http.get('lvyoushuju/visualization', { params: { city: this.city, metric: this.metric, limit: 12 } }).then(res => {
        if (res.data.code === 0) {
          this.list = res.data.data || []
          this.maxValue = Math.max(...this.list.map(item => Number(item.value) || 0), 1)
        }
      })
    },
    percent(value) {
      return `${Math.max((Number(value) || 0) / this.maxValue * 100, 8)}%`
    }
  }
}
</script>

<style scoped lang="scss">
.page { width: 1200px; margin: 20px auto; }
.header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.title { font-size: 28px; font-weight: 700; color: #2f6f42; }
.filters { display: flex; gap: 16px; margin-bottom: 24px; }
.bars { padding: 24px; background: #fff; border-radius: 14px; box-shadow: 0 8px 24px rgba(33, 72, 50, 0.08); }
.bar-row { display: grid; grid-template-columns: 260px 1fr 100px; align-items: center; gap: 16px; margin-bottom: 16px; }
.bar-label { color: #22342c; font-weight: 600; }
.bar-track { overflow: hidden; height: 18px; background: #edf3ea; border-radius: 999px; }
.bar-fill { height: 100%; border-radius: 999px; background: linear-gradient(90deg, #3f7f58, #98c58d); }
.bar-value { text-align: right; color: #2f6f42; font-weight: 700; }
</style>
