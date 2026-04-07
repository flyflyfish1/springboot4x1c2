import pandas as pd
import matplotlib.pyplot as plt
import numpy as np
from wordcloud import WordCloud
import jieba
from collections import Counter
import warnings
warnings.filterwarnings('ignore')

# 设置中文字体
plt.rcParams['font.sans-serif'] = ['SimHei', 'Microsoft YaHei']
plt.rcParams['axes.unicode_minus'] = False

# 读取数据
print("正在读取数据...")
df = pd.read_excel('data.xlsx')
print(f"原始数据加载完成，共 {len(df)} 条记录\n")

# ==================== 数据清洗 ====================
print("="*50)
print("开始数据清洗...")
print("="*50)

# 1. 删除重复数据
df_before = len(df)
df = df.drop_duplicates()
df_after = len(df)
print(f"删除重复数据: {df_before - df_after} 条")

# 2. 处理缺失值
missing_level = df['level'].isnull().sum()
missing_detail = df['detailaddress'].isnull().sum()
print(f"level字段缺失值: {missing_level} 条（填充为'未知等级'）")
print(f"detailaddress字段缺失值: {missing_detail} 条（填充为'未知地址'）")
df['level'] = df['level'].fillna('未知等级')
df['detailaddress'] = df['detailaddress'].fillna('未知地址')

# 3. 处理异常值 - price字段
Q1_price = df['price'].quantile(0.25)
Q3_price = df['price'].quantile(0.75)
IQR_price = Q3_price - Q1_price
lower_bound_price = Q1_price - 1.5 * IQR_price
upper_bound_price = Q3_price + 1.5 * IQR_price
price_outliers = ((df['price'] < lower_bound_price) | (df['price'] > upper_bound_price)).sum()
print(f"\n价格异常值检测:")
print(f"  正常范围: {lower_bound_price:.2f} - {upper_bound_price:.2f}")
print(f"  异常值数量: {price_outliers} 条")

# 4. 处理异常值 - sales字段
Q1_sales = df['sales'].quantile(0.25)
Q3_sales = df['sales'].quantile(0.75)
IQR_sales = Q3_sales - Q1_sales
lower_bound_sales = Q1_sales - 1.5 * IQR_sales
upper_bound_sales = Q3_sales + 1.5 * IQR_sales
sales_outliers = ((df['sales'] < lower_bound_sales) | (df['sales'] > upper_bound_sales)).sum()
print(f"\n销量异常值检测:")
print(f"  正常范围: {lower_bound_sales:.2f} - {upper_bound_sales:.2f}")
print(f"  异常值数量: {sales_outliers} 条")

# 5. 处理异常值 - score字段
score_outliers_count = 0
if 'score' in df.columns:
    Q1_score = df['score'].quantile(0.25)
    Q3_score = df['score'].quantile(0.75)
    IQR_score = Q3_score - Q1_score
    lower_bound_score = Q1_score - 1.5 * IQR_score
    upper_bound_score = Q3_score + 1.5 * IQR_score
    # 评分通常在0-5之间，使用业务规则过滤
    score_outliers = ((df['score'] < 0) | (df['score'] > 5))
    score_outliers_count = score_outliers.sum()
    print(f"\n评分异常值检测:")
    print(f"  异常范围: < 0 或 > 5")
    print(f"  异常值数量: {score_outliers_count} 条")

# 6. 过滤异常值（保留合理范围内的数据用于分析）
df_clean = df.copy()

# 过滤评分异常值（0-5范围）
if 'score' in df.columns:
    df_clean = df_clean[(df_clean['score'] >= 0) & (df_clean['score'] <= 5)]

# 过滤价格异常值（使用IQR方法）
if 'price' in df.columns:
    Q1_price_clean = df_clean['price'].quantile(0.25)
    Q3_price_clean = df_clean['price'].quantile(0.75)
    IQR_price_clean = Q3_price_clean - Q1_price_clean
    lower_price = max(0, Q1_price_clean - 1.5 * IQR_price_clean)  # 价格不能为负
    upper_price = Q3_price_clean + 1.5 * IQR_price_clean
    price_before = len(df_clean)
    df_clean = df_clean[(df_clean['price'] >= lower_price) & (df_clean['price'] <= upper_price)]
    print(f"\n价格异常值过滤: 移除 {price_before - len(df_clean)} 条")

# 过滤销量异常值（使用IQR方法）
if 'sales' in df.columns:
    Q1_sales_clean = df_clean['sales'].quantile(0.25)
    Q3_sales_clean = df_clean['sales'].quantile(0.75)
    IQR_sales_clean = Q3_sales_clean - Q1_sales_clean
    lower_sales = max(0, Q1_sales_clean - 1.5 * IQR_sales_clean)  # 销量不能为负
    upper_sales = Q3_sales_clean + 1.5 * IQR_sales_clean
    sales_before = len(df_clean)
    df_clean = df_clean[(df_clean['sales'] >= lower_sales) & (df_clean['sales'] <= upper_sales)]
    print(f"销量异常值过滤: 移除 {sales_before - len(df_clean)} 条")

print(f"\n数据清洗完成！")
print(f"原始数据: {len(df)} 条")
print(f"清洗后数据: {len(df_clean)} 条")
print(f"移除异常数据: {len(df) - len(df_clean)} 条")
print("="*50)
print()

# 使用清洗后的数据进行后续分析
df = df_clean

# ==================== 1. 等级饼图占比 ====================
print("正在生成等级饼图...")
level_data = df['level'].value_counts()
plt.figure(figsize=(10, 8))
colors = ['#ff9999', '#66b3ff', '#99ff99', '#ffcc99', '#c2c2f0']
wedges, texts, autotexts = plt.pie(level_data.values, labels=level_data.index, autopct='%1.1f%%',
                                     colors=colors[:len(level_data)], startangle=90, textprops={'fontsize': 12})
for autotext in autotexts:
    autotext.set_fontsize(11)
plt.title('景点等级分布占比', fontsize=16, fontweight='bold', pad=20)
plt.tight_layout()
plt.savefig('1_等级饼图.png', dpi=300, bbox_inches='tight')
plt.close()
print("✓ 等级饼图已保存\n")

# ==================== 2. 城市景点数量柱状图（前10城市）====================
print("正在生成城市景点数量柱状图...")
city_top10 = df['city'].value_counts().head(10)
plt.figure(figsize=(12, 6))
bars = plt.bar(range(len(city_top10)), city_top10.values, color='#5DADE2', edgecolor='white', linewidth=0.5)
plt.xticks(range(len(city_top10)), city_top10.index, rotation=45, ha='right', fontsize=10)
plt.xlabel('城市', fontsize=12)
plt.ylabel('景点数量', fontsize=12)
plt.title('景点数量最多的前10个城市', fontsize=16, fontweight='bold', pad=15)
plt.grid(axis='y', alpha=0.3, linestyle='--')

# 在柱状图上添加数值标签
for bar, value in zip(bars, city_top10.values):
    plt.text(bar.get_x() + bar.get_width()/2., bar.get_height() + 5,
             str(value), ha='center', va='bottom', fontsize=9)

plt.tight_layout()
plt.savefig('2_城市景点数量柱状图.png', dpi=300, bbox_inches='tight')
plt.close()
print("✓ 城市景点数量柱状图已保存\n")

# ==================== 3. 价格区间分析图 ====================
print("正在生成价格区间分析图...")
# 根据实际数据动态设置价格区间
max_price = df['price'].max()
if max_price <= 100:
    price_ranges = [0, 20, 40, 60, 80, 100]
    range_labels = ['0-20', '20-40', '40-60', '60-80', '80-100']
elif max_price <= 300:
    price_ranges = [0, 50, 100, 150, 200, 250]
    range_labels = ['0-50', '50-100', '100-150', '150-200', '200-250']
else:
    price_ranges = [0, 50, 100, 200, 300, 500]
    range_labels = ['0-50', '50-100', '100-200', '200-300', '300-500']

# 确保最后一个区间包含最大值
if max_price > price_ranges[-1]:
    price_ranges.append(max_price + 1)
    range_labels.append(f'{price_ranges[-2]}+')

df['price_range'] = pd.cut(df['price'], bins=price_ranges, labels=range_labels, right=False)
price_dist = df['price_range'].value_counts().sort_index()

plt.figure(figsize=(12, 6))
colors_gradient = ['#AED6F1', '#85C1E9', '#5DADE2', '#3498DB', '#2E86C1', '#2874A6', '#1B4F72']
bars = plt.bar(range(len(price_dist)), price_dist.values, color=colors_gradient[:len(price_dist)], 
               edgecolor='white', linewidth=0.5)
plt.xticks(range(len(price_dist)), price_dist.index, fontsize=11)
plt.xlabel('价格区间（元）', fontsize=12)
plt.ylabel('景点数量', fontsize=12)
plt.title('景点价格区间分布', fontsize=16, fontweight='bold', pad=15)
plt.grid(axis='y', alpha=0.3, linestyle='--')

# 添加数值标签
for bar, value in zip(bars, price_dist.values):
    plt.text(bar.get_x() + bar.get_width()/2., bar.get_height() + 5,
             str(value), ha='center', va='bottom', fontsize=9)

plt.tight_layout()
plt.savefig('3_价格区间分析图.png', dpi=300, bbox_inches='tight')
plt.close()
print("✓ 价格区间分析图已保存\n")

# ==================== 4. 城市名称词云图 ====================
print("正在生成城市词云图...")
# 统计城市频率
city_counter = Counter(df['city'].tolist())

try:
    wordcloud = WordCloud(
        font_path='C:/Windows/Fonts/simhei.ttf',
        width=1200,
        height=800,
        background_color='white',
        max_words=100,
        colormap='viridis'
    ).generate_from_frequencies(city_counter)
    
    plt.figure(figsize=(14, 10))
    plt.imshow(wordcloud, interpolation='bilinear')
    plt.axis('off')
    plt.title('城市词云图', fontsize=16, fontweight='bold', pad=20)
    plt.tight_layout()
    plt.savefig('4_城市词云图.png', dpi=300, bbox_inches='tight')
    plt.close()
    print("✓ 城市词云图已保存\n")
except Exception as e:
    print(f"词云图生成失败: {e}")
    print("尝试使用备用方法...\n")
    # 备用方案：使用柱状图代替
    city_top20 = df['city'].value_counts().head(20)
    plt.figure(figsize=(14, 8))
    bars = plt.bar(range(len(city_top20)), city_top20.values, color='#8E44AD', 
                   edgecolor='white', linewidth=0.5)
    plt.xticks(range(len(city_top20)), city_top20.index, rotation=45, ha='right', fontsize=10)
    plt.xlabel('城市', fontsize=12)
    plt.ylabel('景点数量', fontsize=12)
    plt.title('城市景点分布（Top20）', fontsize=16, fontweight='bold', pad=15)
    plt.grid(axis='y', alpha=0.3, linestyle='--')
    plt.tight_layout()
    plt.savefig('4_城市词云图.png', dpi=300, bbox_inches='tight')
    plt.close()
    print("✓ 城市分布图已保存（替代词云图）\n")

# ==================== 5. 评分分析图 ====================
print("正在生成评分分析图...")
fig, axes = plt.subplots(1, 2, figsize=(16, 6))

# 5.1 评分分布直方图
score_clean = df['score'].dropna()
axes[0].hist(score_clean, bins=30, color='#AF7AC5', edgecolor='white', alpha=0.8)
axes[0].axvline(score_clean.mean(), color='red', linestyle='--', linewidth=2, label=f'平均分: {score_clean.mean():.2f}')
axes[0].axvline(score_clean.median(), color='orange', linestyle='--', linewidth=2, label=f'中位数: {score_clean.median():.2f}')
axes[0].set_xlabel('评分', fontsize=12)
axes[0].set_ylabel('频次', fontsize=12)
axes[0].set_title('景点评分分布直方图', fontsize=14, fontweight='bold')
axes[0].legend(fontsize=10)
axes[0].grid(axis='y', alpha=0.3, linestyle='--')

# 5.2 评分箱线图
box = axes[1].boxplot(score_clean, vert=True, patch_artist=True,
                      boxprops=dict(facecolor='#D2B4DE', color='#8E44AD'),
                      medianprops=dict(color='red', linewidth=2),
                      whiskerprops=dict(color='#8E44AD'),
                      capprops=dict(color='#8E44AD'),
                      flierprops=dict(markerfacecolor='#8E44AD', marker='o', markersize=5))
axes[1].set_ylabel('评分', fontsize=12)
axes[1].set_title('景点评分箱线图', fontsize=14, fontweight='bold')
axes[1].grid(axis='y', alpha=0.3, linestyle='--')

plt.suptitle('景点评分综合分析', fontsize=16, fontweight='bold', y=1.02)
plt.tight_layout()
plt.savefig('5_评分分析图.png', dpi=300, bbox_inches='tight')
plt.close()
print("✓ 评分分析图已保存\n")

# ==================== 6. 销量Top15景点横向柱状图 ====================
print("正在生成销量Top15景点图...")
top15_sales = df.nlargest(15, 'sales')[['name', 'sales']]
top15_sales = top15_sales.sort_values('sales', ascending=True)

plt.figure(figsize=(12, 8))
bars = plt.barh(range(len(top15_sales)), top15_sales['sales'].values, color='#F39C12', 
                edgecolor='white', linewidth=0.5)
plt.yticks(range(len(top15_sales)), top15_sales['name'].values, fontsize=9)
plt.xlabel('销量', fontsize=12)
plt.ylabel('景点名称', fontsize=12)
plt.title('销量最高的前15个景点', fontsize=16, fontweight='bold', pad=15)
plt.grid(axis='x', alpha=0.3, linestyle='--')

# 添加数值标签
for bar, value in zip(bars, top15_sales['sales'].values):
    plt.text(bar.get_width() + 100, bar.get_y() + bar.get_height()/2.,
             str(int(value)), ha='left', va='center', fontsize=8)

plt.tight_layout()
plt.savefig('6_销量Top15景点.png', dpi=300, bbox_inches='tight')
plt.close()
print("✓ 销量Top15景点图已保存\n")

# ==================== 7. 不同等级景点的平均价格和评分对比 ====================
print("正在生成等级-价格-评分对比图...")
level_stats = df.groupby('level').agg({
    'price': 'mean',
    'score': 'mean',
    'sales': 'mean'
}).round(2).reset_index()
level_stats = level_stats.dropna()

fig, ax1 = plt.subplots(figsize=(12, 6))
x = range(len(level_stats))
width = 0.35

bars1 = ax1.bar([i - width/2 for i in x], level_stats['price'].values, width, 
                label='平均价格', color='#3498DB', alpha=0.8)
ax1.set_xlabel('景点等级', fontsize=12)
ax1.set_ylabel('平均价格（元）', fontsize=12, color='#3498DB')
ax1.tick_params(axis='y', labelcolor='#3498DB')
ax1.set_xticks(list(x))
ax1.set_xticklabels(level_stats['level'].values, fontsize=11)
ax1.legend(loc='upper left', fontsize=10)
ax1.grid(axis='y', alpha=0.3, linestyle='--')

ax2 = ax1.twinx()
bars2 = ax2.bar([i + width/2 for i in x], level_stats['score'].values, width, 
                label='平均评分', color='#E74C3C', alpha=0.8)
ax2.set_ylabel('平均评分', fontsize=12, color='#E74C3C')
ax2.tick_params(axis='y', labelcolor='#E74C3C')
ax2.legend(loc='upper right', fontsize=10)

plt.title('不同等级景点的平均价格与评分对比', fontsize=16, fontweight='bold', pad=15)
plt.tight_layout()
plt.savefig('7_等级价格评分对比.png', dpi=300, bbox_inches='tight')
plt.close()
print("✓ 等级-价格-评分对比图已保存\n")

# ==================== 8. 免费vs收费景点占比 ====================
print("正在生成免费vs收费景点占比图...")
isfree_count = df['isfree'].value_counts()
isfree_labels = ['收费景点', '免费景点']
explode = (0.05, 0.05)

plt.figure(figsize=(10, 8))
colors = ['#E74C3C', '#2ECC71']
wedges, texts, autotexts = plt.pie(isfree_count.values, labels=isfree_labels, autopct='%1.1f%%',
                                     explode=explode, colors=colors, startangle=90,
                                     textprops={'fontsize': 13}, shadow=True)
for autotext in autotexts:
    autotext.set_fontsize(12)
    autotext.set_fontweight('bold')
plt.title('免费与收费景点占比', fontsize=16, fontweight='bold', pad=20)
plt.tight_layout()
plt.savefig('8_免费收费景点占比.png', dpi=300, bbox_inches='tight')
plt.close()
print("✓ 免费vs收费景点占比图已保存\n")

# ==================== 9. 各城市平均评分热力图（Top10城市）====================
print("正在生成城市平均评分热力图...")
city_score = df.groupby('city')['score'].mean().nlargest(10).reset_index()
city_score.columns = ['城市', '平均评分']

plt.figure(figsize=(12, 6))
colors_heat = plt.cm.YlOrRd(np.linspace(0.3, 0.9, len(city_score)))
bars = plt.bar(range(len(city_score)), city_score['平均评分'].values, color=colors_heat, 
               edgecolor='white', linewidth=0.5)
plt.xticks(range(len(city_score)), city_score['城市'].values, rotation=45, ha='right', fontsize=10)
plt.xlabel('城市', fontsize=12)
plt.ylabel('平均评分', fontsize=12)
plt.title('平均评分最高的前10个城市', fontsize=16, fontweight='bold', pad=15)
plt.grid(axis='y', alpha=0.3, linestyle='--')
plt.ylim(0, 5)

# 添加数值标签
for bar, value in zip(bars, city_score['平均评分'].values):
    plt.text(bar.get_x() + bar.get_width()/2., bar.get_height() + 0.05,
             f'{value:.2f}', ha='center', va='bottom', fontsize=9)

plt.tight_layout()
plt.savefig('9_城市平均评分热力图.png', dpi=300, bbox_inches='tight')
plt.close()
print("✓ 城市平均评分热力图已保存\n")

# ==================== 10. 价格与销量的散点图 ====================
print("正在生成价格-销量散点图...")
plt.figure(figsize=(12, 8))
scatter = plt.scatter(df['price'], df['sales'], c=df['score'], cmap='viridis', 
                     alpha=0.6, s=50, edgecolors='gray', linewidth=0.5)
plt.colorbar(scatter, label='评分')
plt.xlabel('价格（元）', fontsize=12)
plt.ylabel('销量', fontsize=12)
plt.title('景点价格与销量关系（颜色表示评分）', fontsize=16, fontweight='bold', pad=15)
plt.grid(alpha=0.3, linestyle='--')
plt.tight_layout()
plt.savefig('10_价格销量散点图.png', dpi=300, bbox_inches='tight')
plt.close()
print("✓ 价格-销量散点图已保存\n")

print("="*50)
print("所有图表生成完成！")
print("="*50)
print("\n生成的文件列表：")
print("1. 1_等级饼图.png")
print("2. 2_城市景点数量柱状图.png")
print("3. 3_价格区间分析图.png")
print("4. 4_城市词云图.png")
print("5. 5_评分分析图.png")
print("6. 6_销量Top15景点.png")
print("7. 7_等级价格评分对比.png")
print("8. 8_免费收费景点占比.png")
print("9. 9_城市平均评分热力图.png")
print("10. 10_价格销量散点图.png")
