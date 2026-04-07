<template>
<div class="page">
  <div class="header">
    <div class="title">排行榜</div>
    <el-button @click="$router.push('/index/lvyoushuju')">返回检索</el-button>
  </div>
  <div class="boards" v-if="boards">
    <div class="board" v-for="board in boardList" :key="board.key">
      <div class="board-title">{{ board.title }}</div>
      <div class="board-item" v-for="(item, index) in boards[board.key]" :key="index">
        <div class="rank">{{ index + 1 }}</div>
        <div class="name">{{ item.name }}</div>
        <div class="value">{{ formatValue(item.value) }}</div>
      </div>
    </div>
  </div>
</div>
</template>

<script>
export default {
  data() {
    return {
      boards: null,
      boardList: [
        { key: 'hotSpotRanking', title: '景点访问热度排名' },
        { key: 'commentRanking', title: '景点评论人数排名' },
        { key: 'compositeRanking', title: '景点综合排名' },
        { key: 'cityHotRanking', title: '城市热度排名' },
        { key: 'cityLivabilityRanking', title: '城市宜居性排名' }
      ]
    }
  },
  created() {
    this.$http.get('lvyoushuju/rankings').then(res => {
      if (res.data.code === 0) this.boards = res.data.data
    })
  },
  methods: {
    formatValue(value) {
      return Number(value || 0).toFixed(0)
    }
  }
}
</script>

<style scoped lang="scss">
.page { width: 1200px; margin: 20px auto; }
.header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.title { font-size: 28px; font-weight: 700; color: #2f6f42; }
.boards { display: grid; grid-template-columns: repeat(2, 1fr); gap: 20px; }
.board { padding: 20px; background: #fff; border-radius: 14px; box-shadow: 0 8px 24px rgba(33, 72, 50, 0.08); }
.board-title { margin-bottom: 16px; font-size: 20px; font-weight: 700; color: #22342c; }
.board-item { display: grid; grid-template-columns: 40px 1fr 100px; gap: 12px; align-items: center; padding: 10px 0; border-bottom: 1px solid #edf3ea; }
.rank { width: 30px; height: 30px; line-height: 30px; text-align: center; border-radius: 50%; background: #edf7ea; color: #2f6f42; font-weight: 700; }
.name { color: #314339; font-weight: 600; }
.value { text-align: right; color: #1f7d4d; font-weight: 700; }
</style>
