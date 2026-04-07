<template>
  <div class="mod-config">
    <el-form :inline="true" :model="searchForm">
      <el-form-item label="城市">
        <el-select v-model="searchForm.diming" clearable placeholder="城市">
          <el-option v-for="item in dimingOptions" :key="item" :label="item" :value="item"></el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="景点名">
        <el-input v-model="searchForm.jingdianming" clearable placeholder="景点名"></el-input>
      </el-form-item>
      <el-form-item label="景点类型">
        <el-input v-model="searchForm.jingdianleixing" clearable placeholder="景点类型"></el-input>
      </el-form-item>
      <el-form-item label="热度">
        <el-input-number v-model="searchForm.redu_start" :min="0"></el-input-number>
        <span class="sep">-</span>
        <el-input-number v-model="searchForm.redu_end" :min="0"></el-input-number>
      </el-form-item>
      <el-form-item label="宜居性">
        <el-input-number v-model="searchForm.yijuxing_start" :min="0"></el-input-number>
        <span class="sep">-</span>
        <el-input-number v-model="searchForm.yijuxing_end" :min="0"></el-input-number>
      </el-form-item>
      <el-form-item label="排序字段">
        <el-select v-model="searchForm.sort">
          <el-option label="热度" value="redu"></el-option>
          <el-option label="评论人数" value="pinglunrenshu"></el-option>
          <el-option label="排名" value="paiming"></el-option>
          <el-option label="宜居性" value="yijuxing"></el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="排序">
        <el-select v-model="searchForm.order">
          <el-option label="降序" value="desc"></el-option>
          <el-option label="升序" value="asc"></el-option>
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="getDataList()">查询</el-button>
        <el-button @click="reset">重置</el-button>
        <el-button v-if="isAuth('lvyoushuju','新增')" type="success" @click="addOrUpdateHandler()">新增</el-button>
        <el-button v-if="isAuth('lvyoushuju','删除')" type="danger" :disabled="dataListSelections.length<=0" @click="deleteHandler()">删除</el-button>
        <el-button v-if="isAuth('lvyoushuju','导入')" @click="importVisible=true">导入Excel/CSV</el-button>
        <el-button v-if="isAuth('lvyoushuju','导出')" type="warning" @click="exportData('csv')">导出CSV</el-button>
        <el-button v-if="isAuth('lvyoushuju','导出')" type="warning" @click="exportData('xls')">导出Excel</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="dataList" v-loading="dataListLoading" border @selection-change="selectionChangeHandler">
      <el-table-column type="selection" width="50"></el-table-column>
      <el-table-column prop="diming" label="城市"></el-table-column>
      <el-table-column prop="jingdianming" label="景点名"></el-table-column>
      <el-table-column prop="jingdianleixing" label="景点类型"></el-table-column>
      <el-table-column prop="paiming" label="排名"></el-table-column>
      <el-table-column prop="pinglunrenshu" label="评论人数"></el-table-column>
      <el-table-column prop="redu" label="热度"></el-table-column>
      <el-table-column prop="yijuxing" label="宜居性"></el-table-column>
      <el-table-column prop="clicknum" label="点击次数"></el-table-column>
      <el-table-column label="操作" width="260">
        <template slot-scope="scope">
          <el-button size="mini" @click="addOrUpdateHandler(scope.row.id, 'info')">查看</el-button>
          <el-button size="mini" type="primary" @click="addOrUpdateHandler(scope.row.id)">修改</el-button>
          <el-button size="mini" type="danger" @click="deleteHandler(scope.row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      @size-change="sizeChangeHandle"
      @current-change="currentChangeHandle"
      :current-page="pageIndex"
      :page-sizes="[10,20,30,50]"
      :page-size="pageSize"
      :total="totalPage"
      layout="total, sizes, prev, pager, next, jumper">
    </el-pagination>

    <add-or-update v-if="addOrUpdateFlag" :parent="this" ref="addOrUpdate"></add-or-update>

    <el-dialog title="导入数据" :visible.sync="importVisible" width="520px">
      <excel-file-upload
        tip="支持Excel或CSV文件"
        action="lvyoushuju/importData"
        :limit="1"
        @change="importDone">
      </excel-file-upload>
    </el-dialog>
  </div>
</template>

<script>
import AddOrUpdate from './add-or-update-enhanced'
export default {
  components: { AddOrUpdate },
  data() {
    return {
      searchForm: { diming: '', jingdianming: '', jingdianleixing: '', redu_start: null, redu_end: null, yijuxing_start: null, yijuxing_end: null, sort: 'redu', order: 'desc' },
      dimingOptions: [],
      dataList: [],
      pageIndex: 1,
      pageSize: 10,
      totalPage: 0,
      dataListLoading: false,
      dataListSelections: [],
      addOrUpdateFlag: false,
      showFlag: true,
      importVisible: false
    }
  },
  created() {
    this.$http.get('lvyoushuju/rankings').then(res => { if (res.data.code === 0) this.dimingOptions = res.data.data.cities || [] })
    this.getDataList()
  },
  methods: {
    buildParams() {
      const params = { page: this.pageIndex, limit: this.pageSize, sort: this.searchForm.sort, order: this.searchForm.order }
      Object.keys(this.searchForm).forEach(key => {
        const value = this.searchForm[key]
        if (value !== '' && value !== null && value !== undefined) params[key] = key === 'jingdianming' ? '%' + value + '%' : value
      })
      return params
    },
    getDataList() {
      this.dataListLoading = true
      this.$http({ url: 'lvyoushuju/page', method: 'get', params: this.buildParams() }).then(({ data }) => {
        if (data && data.code === 0) {
          this.dataList = data.data.list
          this.totalPage = data.data.total
        }
        this.dataListLoading = false
      })
    },
    reset() {
      this.searchForm = { diming: '', jingdianming: '', jingdianleixing: '', redu_start: null, redu_end: null, yijuxing_start: null, yijuxing_end: null, sort: 'redu', order: 'desc' }
      this.pageIndex = 1
      this.getDataList()
    },
    exportData(format) {
      const query = Object.keys(this.buildParams()).map(key => `${encodeURIComponent(key)}=${encodeURIComponent(this.buildParams()[key])}`).join('&')
      window.open(`${this.$base.url}lvyoushuju/exportData?format=${format}&mode=query&${query}`)
    },
    importDone() {
      this.importVisible = false
      this.getDataList()
    },
    sizeChangeHandle(val) { this.pageSize = val; this.pageIndex = 1; this.getDataList() },
    currentChangeHandle(val) { this.pageIndex = val; this.getDataList() },
    selectionChangeHandler(val) { this.dataListSelections = val },
    addOrUpdateHandler(id, type) {
      this.showFlag = false
      this.addOrUpdateFlag = true
      this.$nextTick(() => this.$refs.addOrUpdate.init(id, type || 'edit'))
    },
    deleteHandler(id) {
      const ids = id ? [Number(id)] : this.dataListSelections.map(item => Number(item.id))
      this.$http({ url: 'lvyoushuju/delete', method: 'post', data: ids }).then(({ data }) => {
        if (data && data.code === 0) this.getDataList()
      })
    }
  }
}
</script>

<style scoped>
.sep { display: inline-block; padding: 0 8px; }
</style>
