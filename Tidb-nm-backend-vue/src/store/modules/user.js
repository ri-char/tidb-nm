import api from '@/api'

import useRouteStore from './route'
import useMenuStore from './menu'

const useUserStore = defineStore(
    // 唯一ID
    'user',
    {
        state: () => ({
            account: localStorage.account || '',
            token: localStorage.token || '',
            failure_time: localStorage.failure_time || '',
            permissions: []
        }),
        getters: {
            isLogin: state => {
                let retn = false
                if (state.account !== '') {
                    retn = true
                }
                return retn
            }
        },
        actions: {
            login(data) {
                return new Promise((resolve, reject) => {
                    // 通过 mock 进行登录
                    api.get('api/login',{
                        params: data
                    }).then(() => {
                        localStorage.setItem('account', data.username)
                        this.account = data.username
                        resolve()
                    }).catch(error => {
                        reject(error)
                    })
                })
            },
            logout() {
                return new Promise(resolve => {
                    const routeStore = useRouteStore()
                    const menuStore = useMenuStore()
                    localStorage.removeItem('account')
                    this.account = ''
                    routeStore.removeRoutes()
                    menuStore.setActived(0)
                    resolve()
                })
            }
        }
    }
)

export default useUserStore
