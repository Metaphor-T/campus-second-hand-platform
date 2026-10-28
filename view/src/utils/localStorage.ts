const ROLE_KEY= "ROLE" // 角色KEY
const TOKEN_KEY= "TOKEN" // Token KEY
const LAST_VISITED_PATH_KEY = "LAST_VISITED_PATH"
const USER_INFO_KEY = "USER_INFO"
const SIDEBAR_WIDTH_KEY = "SIDEBAR_WIDTH"
const SIDEBAR_COLLAPSED_KEY = "SIDEBAR_COLLAPSED"

export const setRole = (role: string)=>{
    localStorage.setItem(ROLE_KEY,role)
}
export const getRole = ()=>{
    return localStorage.getItem(ROLE_KEY)
}

export const clearRole = () => {
    localStorage.removeItem(ROLE_KEY)
}

export const setToken = (token: string)=>{
    localStorage.setItem(TOKEN_KEY,token)
}
export const getToken = ()=>{
    return localStorage.getItem(TOKEN_KEY)
}

export const clearToken = () => {
    localStorage.removeItem(TOKEN_KEY)
}

// 最后访问路径
export const setLastVisitedPath = (path: string) => {
    localStorage.setItem(LAST_VISITED_PATH_KEY, path)
}

export const getLastVisitedPath = (): string | null => {
    return localStorage.getItem(LAST_VISITED_PATH_KEY)
}

export const clearLastVisitedPath = () => {
    localStorage.removeItem(LAST_VISITED_PATH_KEY)
}

// 用户信息
export const setUserInfo = (info: object) => {
    localStorage.setItem(USER_INFO_KEY, JSON.stringify(info))
}

export const getUserInfo = (): object | null => {
    const info = localStorage.getItem(USER_INFO_KEY)
    return info ? JSON.parse(info) : null
}

export const clearUserInfo = () => {
    localStorage.removeItem(USER_INFO_KEY)
}

// 侧边栏宽度
export const setSidebarWidth = (width: number) => {
    localStorage.setItem(SIDEBAR_WIDTH_KEY, String(width))
}

export const getSidebarWidth = (): number | null => {
    const width = localStorage.getItem(SIDEBAR_WIDTH_KEY)
    return width ? Number(width) : null
}

// 侧边栏折叠状态
export const setSidebarCollapsed = (collapsed: boolean) => {
    localStorage.setItem(SIDEBAR_COLLAPSED_KEY, String(collapsed))
}

export const getSidebarCollapsed = (): boolean | null => {
    const collapsed = localStorage.getItem(SIDEBAR_COLLAPSED_KEY)
    return collapsed === 'true' ? true : collapsed === 'false' ? false : null
}