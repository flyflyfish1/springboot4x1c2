<template>
<div class="spot-page">
  <div class="toolbar">
    <div class="title">景点检索</div>
    <div class="actions">
      <el-button type="success" @click="$router.push('/index/recommend')">个性化推荐</el-button>
      <el-button type="primary" @click="$router.push('/index/visualization')">城市可视化</el-button>
      <el-button type="warning" @click="$router.push('/index/ranking')">排行榜</el-button>
    </div>
  </div>

  <el-form :inline="true" :model="formSearch" class="search-form">
    <el-form-item label="城市">
      <el-select v-model="formSearch.diming" clearable placeholder="选择城市">
        <el-option v-for="item in dimingOptions" :key="item" :label="item" :value="item"></el-option>
      </el-select>
    </el-form-item>
    <el-form-item label="景点名">
      <el-input v-model="formSearch.jingdianming" clearable placeholder="输入名称"></el-input>
    </el-form-item>
    <el-form-item label="景点类型">
      <el-input v-model="formSearch.jingdianleixing" clearable placeholder="如自然/人文"></el-input>
    </el-form-item>
    <el-form-item label="排名">
      <el-input-number v-model="formSearch.paiming_start" :min="0" controls-position="right"></el-input-number>
      <span class="range-sep">-</span>
      <el-input-number v-model="formSearch.paiming_end" :min="0" controls-position="right"></el-input-number>
    </el-form-item>
    <el-form-item label="评论人数">
      <el-input-number v-model="formSearch.pinglunrenshu_start" :min="0" controls-position="right"></el-input-number>
      <span class="range-sep">-</span>
      <el-input-number v-model="formSearch.pinglunrenshu_end" :min="0" controls-position="right"></el-input-number>
    </el-form-item>
    <el-form-item label="热度">
      <el-input-number v-model="formSearch.redu_start" :min="0" controls-position="right"></el-input-number>
      <span class="range-sep">-</span>
      <el-input-number v-model="formSearch.redu_end" :min="0" controls-position="right"></el-input-number>
    </el-form-item>
    <el-form-item label="宜居性">
      <el-input-number v-model="formSearch.yijuxing_start" :min="0" controls-position="right"></el-input-number>
      <span class="range-sep">-</span>
      <el-input-number v-model="formSearch.yijuxing_end" :min="0" controls-position="right"></el-input-number>
    </el-form-item>
    <el-form-item label="排序字段">
      <el-select v-model="formSearch.sort">
        <el-option label="评论人数" value="pinglunrenshu"></el-option>
        <el-option label="排名" value="paiming"></el-option>
        <el-option label="热度" value="redu"></el-option>
        <el-option label="宜居性" value="yijuxing"></el-option>
        <el-option label="点击次数" value="clicknum"></el-option>
      </el-select>
    </el-form-item>
    <el-form-item label="排序方式">
      <el-select v-model="formSearch.order">
        <el-option label="降序" value="desc"></el-option>
        <el-option label="升序" value="asc"></el-option>
      </el-select>
    </el-form-item>
    <el-form-item>
      <el-button type="primary" @click="getList(1)">查询</el-button>
      <el-button @click="resetSearch">重置</el-button>
      <el-button type="success" @click="exportData('csv')">导出CSV</el-button>
      <el-button type="warning" @click="exportData('xls')">导出Excel</el-button>
    </el-form-item>
  </el-form>

  <div class="list-grid">
    <div class="spot-card" v-for="item in dataList" :key="item.id" @click="toDetail(item)">
      <img v-if="item.fengmiantupian && item.fengmiantupian.substr(0,4)=='http'" :src="item.fengmiantupian.split(',')[0]" />
      <img v-else :src="baseUrl + (item.fengmiantupian ? item.fengmiantupian.split(',')[0] : '')" />
      <div class="spot-body">
        <div class="spot-name">{{ item.jingdianming }}</div>
        <div class="spot-city">{{ item.diming }}<span v-if="item.jingdianleixing"> / {{ item.jingdianleixing }}</span></div>
        <div class="spot-meta">排名 {{ item.paiming || 0 }} | 评论 {{ item.pinglunrenshu || 0 }}</div>
        <div class="spot-meta">热度 {{ item.redu || 0 }} | 宜居性 {{ item.yijuxing || 0 }}</div>
        <div class="spot-tags" v-if="item.cityTags">{{ item.cityTags }}</div>
      </div>
    </div>
  </div>

  <el-empty v-if="!dataList.length" description="暂无符合条件的景点"></el-empty>
  <el-pagination
    background
    :page-size="pageSize"
    :page-sizes="pageSizes"
    :current-page="page"
    layout="total, prev, pager, next, sizes, jumper"
    :total="total"
    @current-change="curChange"
    @size-change="sizeChange">
  </el-pagination>
</div>
</template>

<script>
export default {
  data() {
    return {
      baseUrl: '',
      dimingOptions: [],
      dataList: [],
      total: 0,
      page: 1,
      pageSize: 12,
      pageSizes: [12, 24, 36, 48],
      formSearch: {
        diming: '',
        jingdianming: '',
        jingdianleixing: '',
        paiming_start: null,
        paiming_end: null,
        pinglunrenshu_start: null,
        pinglunrenshu_end: null,
        redu_start: null,
        redu_end: null,
        yijuxing_start: null,
        yijuxing_end: null,
        sort: 'redu',
        order: 'desc'
      }
    }
  },
  created() {
    this.baseUrl = this.$config.baseUrl
    this.$http.get('lvyoushuju/rankings').then(res => {
      if (res.data.code === 0) this.dimingOptions = res.data.data.cities || []
    })
    this.getList(1)
  },
  methods: {
    buildParams(page) {
      const params = { page: page || this.page, limit: this.pageSize, sort: this.formSearch.sort, order: this.formSearch.order }
      Object.keys(this.formSearch).forEach(key => {
        const value = this.formSearch[key]
        if (value !== '' && value !== null && value !== undefined) params[key] = key === 'jingdianming' ? '%' + value + '%' : value
      })
      return params
    },
    getList(page) {
      this.page = page || this.page
      this.$http.get('lvyoushuju/list', { params: this.buildParams(this.page) }).then(res => {
        if (res.data.code === 0) {
          this.dataList = res.data.data.list
          this.total = res.data.data.total
        }
      })
    },
    resetSearch() {
      this.formSearch = {
        diming: '', jingdianming: '', jingdianleixing: '', paiming_start: null, paiming_end: null,
        pinglunrenshu_start: null, pinglunrenshu_end: null, redu_start: null, redu_end: null,
        yijuxing_start: null, yijuxing_end: null, sort: 'redu', order: 'desc'
      }
      this.getList(1)
    },
    exportData(format) {
      const params = this.buildParams(1)
      const query = Object.keys(params).map(key => `${encodeURIComponent(key)}=${encodeURIComponent(params[key])}`).join('&')
      window.open(`${this.baseUrl}lvyoushuju/exportData?format=${format}&mode=query&${query}`)
    },
    toDetail(item) {
      if (localStorage.getItem('Token')) this.$http.get(`lvyoushuju/behavior/${item.id}`, { params: { type: 'click' } })
      this.$router.push({ path: '/index/lvyoushujuDetail', query: { detailObj: JSON.stringify(item) } })
    },
    curChange(page) { this.getList(page) },
    sizeChange(size) { this.pageSize = size; this.getList(1) }
  }
}
</script>

<style scoped lang="scss">
.spot-page { width: 1200px; margin: 20px auto; }
.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.title { font-size: 28px; font-weight: 700; color: #2f6f42; }
.actions .el-button { margin-left: 10px; }
.search-form { padding: 18px; margin-bottom: 24px; background: #f5fbf3; border: 1px solid #cfe2c5; border-radius: 12px; }
.range-sep { display: inline-block; padding: 0 8px; }
.list-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 18px; margin-bottom: 24px; }
.spot-card { overflow: hidden; cursor: pointer; background: #fff; border-radius: 14px; box-shadow: 0 8px 24px rgba(33, 72, 50, 0.08); transition: transform 0.2s ease, box-shadow 0.2s ease; }
.spot-card:hover { transform: translateY(-4px); box-shadow: 0 12px 30px rgba(33, 72, 50, 0.14); }
.spot-card img { width: 100%; height: 230px; object-fit: cover; }
.spot-body { padding: 14px; }
.spot-name { font-size: 18px; font-weight: 700; color: #22342c; }
.spot-city, .spot-meta, .spot-tags { margin-top: 8px; line-height: 1.6; color: #5f6d64; }
.spot-tags { color: #2d7a4b; font-size: 13px; }
</style>
