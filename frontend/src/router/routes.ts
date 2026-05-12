import type { RouteRecordRaw } from 'vue-router'
const AuthLayout = () => import('@/layouts/AuthLayout.vue')
const MainLayout = () => import('@/layouts/MainLayout.vue')
const AdminLayout = () => import('@/layouts/AdminLayout.vue')
const LoginPage = () => import('@/pages/auth/LoginPage.vue')
const RegisterPage = () => import('@/pages/auth/RegisterPage.vue')
const PersonDashboardPage = () => import('@/pages/person/PersonDashboardPage.vue')
const CompanyDashboardPage = () => import('@/pages/company/CompanyDashboardPage.vue')
const DocumentListPage = () => import('@/pages/document/DocumentListPage.vue')
const DocumentUploadPage = () => import('@/pages/document/DocumentUploadPage.vue')
const ParseTaskPage = () => import('@/pages/document/ParseTaskPage.vue')
const ParseResultPage = () => import('@/pages/document/ParseResultPage.vue')
const GraphPage = () => import('@/pages/graph/GraphPage.vue')
const MatchListPage = () => import('@/pages/match/MatchListPage.vue')
const MatchDetailPage = () => import('@/pages/match/MatchDetailPage.vue')
const AdminHomePage = () => import('@/pages/admin/AdminHomePage.vue')
const AdminUsersPage = () => import('@/pages/admin/AdminUsersPage.vue')
const AdminDocumentsPage = () => import('@/pages/admin/AdminDocumentsPage.vue')
const AdminDataPage = () => import('@/pages/admin/AdminDataPage.vue')
const AdminMatchPage = () => import('@/pages/admin/AdminMatchPage.vue')
const AdminMonitorPage = () => import('@/pages/admin/AdminMonitorPage.vue')
const AdminAuditPage = () => import('@/pages/admin/AdminAuditPage.vue')
const ForbiddenPage = () => import('@/pages/system/ForbiddenPage.vue')
const NotFoundPage = () => import('@/pages/system/NotFoundPage.vue')

export type UserType = 'PERSON' | 'COMPANY' | 'ADMIN'

declare module 'vue-router' {
  interface RouteMeta {
    public?: boolean
    userTypes?: UserType[]
    title?: string
  }
}

export const routes: RouteRecordRaw[] = [
  {
    path: '/auth',
    component: AuthLayout,
    meta: { public: true },
    children: [
      { path: 'login', name: 'Login', redirect: { name: 'LoginPerson' }, meta: { public: true, title: '登录' } },
      { path: 'login/person', name: 'LoginPerson', component: LoginPage, meta: { public: true, title: '个人登录' } },
      { path: 'login/company', name: 'LoginCompany', component: LoginPage, meta: { public: true, title: '企业登录' } },
      { path: 'login/admin', name: 'LoginAdmin', component: LoginPage, meta: { public: true, title: '管理员登录' } },
      { path: 'register', name: 'Register', redirect: { name: 'RegisterPerson' }, meta: { public: true, title: '注册' } },
      { path: 'register/person', name: 'RegisterPerson', component: RegisterPage, meta: { public: true, title: '个人注册' } },
      { path: 'register/company', name: 'RegisterCompany', component: RegisterPage, meta: { public: true, title: '企业注册' } },
    ],
  },
  {
    path: '/',
    redirect: '/auth/login',
  },
  {
    path: '/person',
    component: MainLayout,
    meta: { userTypes: ['PERSON'], title: '个人端' },
    children: [
      { path: 'dashboard', name: 'PersonDashboard', component: PersonDashboardPage, meta: { userTypes: ['PERSON'], title: '工作台' } },
      { path: 'doc/list', name: 'PersonDocList', component: DocumentListPage, meta: { userTypes: ['PERSON'], title: '文档中心' } },
      { path: 'doc/upload', name: 'PersonDocUpload', component: DocumentUploadPage, meta: { userTypes: ['PERSON'], title: '简历上传' } },
      { path: 'doc/task/:docId', name: 'PersonParseTask', component: ParseTaskPage, meta: { userTypes: ['PERSON'], title: '解析任务' } },
      { path: 'doc/result/:docId', name: 'PersonParseResult', component: ParseResultPage, meta: { userTypes: ['PERSON'], title: '解析结果' } },
      { path: 'graph/:subjectId', name: 'PersonGraph', component: GraphPage, meta: { userTypes: ['PERSON'], title: '能力图谱' } },
      { path: 'match/jobs', name: 'PersonMatchList', component: MatchListPage, meta: { userTypes: ['PERSON'], title: '人才市场' } },
      { path: 'match/detail/:recordId', name: 'PersonMatchDetail', component: MatchDetailPage, meta: { userTypes: ['PERSON'], title: '匹配详情' } },
    ],
  },
  {
    path: '/company',
    component: MainLayout,
    meta: { userTypes: ['COMPANY'], title: '企业端' },
    children: [
      { path: 'dashboard', name: 'CompanyDashboard', component: CompanyDashboardPage, meta: { userTypes: ['COMPANY'], title: '工作台' } },
      { path: 'doc/list', name: 'CompanyDocList', component: DocumentListPage, meta: { userTypes: ['COMPANY'], title: '文档中心' } },
      { path: 'doc/upload', name: 'CompanyDocUpload', component: DocumentUploadPage, meta: { userTypes: ['COMPANY'], title: 'JD 上传' } },
      { path: 'doc/task/:docId', name: 'CompanyParseTask', component: ParseTaskPage, meta: { userTypes: ['COMPANY'], title: '解析任务' } },
      { path: 'doc/result/:docId', name: 'CompanyParseResult', component: ParseResultPage, meta: { userTypes: ['COMPANY'], title: '解析结果' } },
      { path: 'graph/:subjectId', name: 'CompanyGraph', component: GraphPage, meta: { userTypes: ['COMPANY'], title: '能力图谱' } },
      { path: 'match/candidates', name: 'CompanyMatchList', component: MatchListPage, meta: { userTypes: ['COMPANY'], title: '人才库推荐' } },
      { path: 'match/detail/:recordId', name: 'CompanyMatchDetail', component: MatchDetailPage, meta: { userTypes: ['COMPANY'], title: '匹配详情' } },
    ],
  },
  {
    path: '/admin',
    component: AdminLayout,
    meta: { userTypes: ['ADMIN'], title: '管理端' },
    children: [
      { path: '', name: 'AdminHome', component: AdminHomePage, meta: { userTypes: ['ADMIN'], title: '概览' } },
      { path: 'users', name: 'AdminUsers', component: AdminUsersPage, meta: { userTypes: ['ADMIN'], title: '用户管理' } },
      { path: 'docs', name: 'AdminDocs', component: AdminDocumentsPage, meta: { userTypes: ['ADMIN'], title: '文档库' } },
      { path: 'data', name: 'AdminData', component: AdminDataPage, meta: { userTypes: ['ADMIN'], title: '数据维护' } },
      { path: 'match', name: 'AdminMatch', component: AdminMatchPage, meta: { userTypes: ['ADMIN'], title: '匹配记录' } },
      { path: 'monitor', name: 'AdminMonitor', component: AdminMonitorPage, meta: { userTypes: ['ADMIN'], title: '运营监控' } },
      { path: 'audit', name: 'AdminAudit', component: AdminAuditPage, meta: { userTypes: ['ADMIN'], title: '日志审计' } },
    ],
  },
  { path: '/403', name: 'Forbidden', component: ForbiddenPage, meta: { public: true, title: '无权限' } },
  { path: '/:pathMatch(.*)*', name: 'NotFound', component: NotFoundPage, meta: { public: true, title: '页面不存在' } },
]
