# 异次元商家 · 商家后台 App

ACG-Faka 发卡系统的原生 Android 商家端。Kotlin + Jetpack Compose。商家即前台登录用户，走 /user/api/* 商家权限接口。

## 功能
- 服务器地址配置（测试环境支持 http 内网 IP）
- 登录（图形验证码 + 谷歌验证码）
- 工作台：订单总数、待发货、商品数、卡密库存、余额
- 订单管理：分页、按订单号搜索、待发货/已发货筛选、手动发货
- 商品管理：分页、搜索、上下架
- 卡密管理：分页列表、批量添加、删除
- 分类管理、账单明细

## 对接的真实接口
- POST /user/api/authentication/login
- GET  /user/captcha/image?action=userLogin
- GET  /user/api/site/info
- POST /user/api/commodity/data、/commodity/save、/commodity/del
- POST /user/api/commodityOrder/data、/commodityOrder/delivery
- POST /user/api/card/data、/card/save、/card/del
- POST /user/api/category/data
- POST /user/api/bill/data
- GET  /user/api/security/personal

## 构建
GitHub Actions 工作流构建 debug APK，产物名 acg-faka-merchant-debug-apk。

## 说明
- 部分写操作（商品编辑、分类增删）依赖服务端参数校验，App 端已按现有接口最小实现。
- 平台超管后台（/admin/api/*）是另一套账号体系，不在本 App 范围。
