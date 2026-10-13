# 异次元后台 · 平台管理员 App

ACG-Faka 平台后台（超管）的原生 Android 客户端。Kotlin + Jetpack Compose。

## 与商家端的区别
- 本 App 走 `/admin/api/*`，是**平台管理员**账号体系，会话名 MANAGE_USER。
- 商家端走 `/user/api/*`，是前台用户体系的商家身份。两者完全独立。

## 功能
- 管理员登录（图形验证码 action=adminLogin + 谷歌两步验证）
- 经营概览：今天/昨天/本周/本月/全部的成交额、订单数、利润、买家数、充值、新增用户、待提现、未付款订单
- 订单管理、商品管理、卡密管理、用户管理
- 提现审核（通过/驳回）
- 操作日志、插件管理（开关）
- 服务器地址配置（构建时固定 + 可覆盖）

## 对接接口
- POST /admin/api/authentication/login
- GET  /user/captcha/image?action=adminLogin
- POST /admin/api/authentication/ping
- GET  /admin/api/dashboard/data?type=0
- POST /admin/api/order/data
- POST /admin/api/commodity/data
- POST /admin/api/card/data
- POST /admin/api/user/data
- POST /admin/api/cash/data、/cash/decide
- POST /admin/api/log/data
- GET  /admin/api/plugin/getPlugins、POST /plugin/setConfig

## 构建
GitHub Actions 构建 debug APK，产物名 acg-faka-admin-debug-apk。
