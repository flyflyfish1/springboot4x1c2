module.exports = {
  lintOnSave: false,
  publicPath: process.env.NODE_ENV === 'development' ? './' : '././',
  outputDir: 'dist',
  configureWebpack: {
    performance: {
      hints: false
    },
    optimization: {
      splitChunks: {
        chunks: 'all'
      }
    }
  },
  css: {
    loaderOptions: {
      sass: {
        sassOptions: {
          outputStyle: 'expanded'
        }
      }
    }
  }
}