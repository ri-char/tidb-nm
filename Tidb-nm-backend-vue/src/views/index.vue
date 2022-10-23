<route>
{
    name: 'dashboard',
    meta: {
        title: "项目说明"
    }
}
</route>

<script setup name="NodeShowList">
import api from '@/api'
import md5 from 'js-md5'

const router = useRouter()
// const route = useRoute()

const scrollbarRef = ref()
const innerRef = ref()
const data = ref({
    loading: false,
    scrollLeft: 0,
    slider: {
        max: 0,
        progress: 0
    },
    areaNum: 1,//区域数量
    nodeData: [],
    linkData: []
})

const { proxy } = getCurrentInstance()

let myChart
let option = {
    title: {
        text: ''
    },
    tooltip: {},
    animationDurationUpdate: 1500,
    animationEasingUpdate: 'quinticInOut',
    series: [
        {
            type: 'graph',
            layout: 'none',
            symbolSize: 50,
            roam: true,
            label: {
                show: true
            },
            edgeSymbol: ['circle', 'arrow'],
            edgeSymbolSize: [4, 10],
            edgeLabel: {
                fontSize: 20,
                color: 'red'
            },
            edgeStyle: {
                color: 'red'
            },
            data: data.value.nodeData,
            links: data.value.linkData,
            tooltip: {
                trigger: 'item',
                formatter: params => {
                    // console.log(params.data)
                    let size,str
                    if (params.data.hasOwnProperty('source')) {
                        size = params.data.label.formatter ?? '0'
                        str = params.data.source + "->"+size +"->" +params.data.target
                    }else {
                        str = params.data.value
                    }
                    return  str
                }
            },
            lineStyle: {
                opacity: 0.9,
                width: 2,
                curveness: 0,
                color: 'blue'
            }
        }
    ]
}

onMounted(() => {
    getDataList()
})

function getDataList() {
    let cacheMap = new Map()
    let otherMap = new Map()
    let color = ['#79bbff', '#79ffa6', '#73767a', '#fa2053', '#50eedb']
    let d = 0,k = 0,f = 0, s = 0, o =0
    let y = 0
    // data.value.nodeData = []
    // data.value.linkData = []
    data.value.slider.max =6000- innerRef.value.clientWidth
    console.log(data.value.slider.max)
    api.get('api/getAllFlow').then(res => {
        data.value.areaNum = res.data.length
        // console.log(res.data.data)
        res.data.data.forEach(item => {
            let i
            if (item.current_ip.indexOf('tidb') === 0) {
                i = 0
                d = d+1
                y = d
            }else if (item.current_ip.indexOf('tikv') === 0) {
                i = 1
                k = k+1
                y = k
            }else if (item.current_ip.indexOf('tiflash') === 0){
                i = 3
                f = f+1
                y = f
            }else if (item.current_ip.indexOf('dashboard') === 0){
                i = 4
                s = s+1
                y = s
            }
            // // console.log(y.get(d))
            // console.log(d)
            cacheMap.set(item.current_ip,{
                name: '',
                value: item.current_ip,
                x: 500*i,
                y: y*200,
                itemStyle: {
                    color: color[i]
                }
            })
            item.ip_list.forEach(it => {
                if (typeof it.detail !== "undefined") {
                    let f = -1
                    it.detail.forEach(index => {
                        f = f+1
                        if (index.ip.indexOf('dashboard') === 0) {
                            i = 4
                            s = 1
                            y = s
                        }else if (index.ip.indexOf('other') === 0){
                            i = 2
                            o = o+1
                            y = o
                        }
                        // console.log(s)
                        // console.log(index.ip)
                        // console.log(y)
                        otherMap.set(index.ip,{
                            value: index.ip,
                            x: 500*i,
                            y: y*200,
                            itemStyle: {
                                color: i ===4 ? color[i]:color[2]
                            }
                        })
                        if ( index.rx !== 0 ) {
                            data.value.linkData.push({
                                source: index.ip,
                                target: item.current_ip,
                                label: {
                                    show: false,
                                    formatter: "接收"+index.rx/5+'bit/s'
                                },
                                lineStyle: {
                                    curveness: 0.2
                                }
                            })
                        }
                        if ( index.tx !== 0 ) {
                            data.value.linkData.push({
                                source: item.current_ip,
                                target: index.ip,
                                label: {
                                    show: false,
                                    formatter: "发送"+index.tx/5+'bit/s'
                                },
                                lineStyle: {
                                    curveness: 0.2
                                }
                            })
                        }
                    })
                }
            })
        })
        // console.log(cacheMap)
        let merged = new Map([...otherMap, ...cacheMap])
        merged.forEach((value, key) => {
            // console.log(value)
            data.value.nodeData.push({
                // name: key,
                value: value.value,
                id: key,
                x: value.x,
                y: value.y,
                itemStyle: value.itemStyle
            })
        })
        merged.clear()
        cacheMap.clear()
        otherMap.clear()
        console.log('|||')
        // console.log(JSON.stringify(data.value.nodeData))
        // console.log(JSON.stringify(data.value.linkData))
        console.log(option)
        myChart = proxy.$echarts.init(document.getElementById('myEcharts'))
        option && myChart.setOption(option)
    })
}
const text = ref('重置')
const disableButton = ref(false)
function refresh() {
    let nowTime = 5
    data.value.nodeData = []
    data.value.linkData = []
    myChart.dispose()
    getDataList()
    let checkFlag = setInterval(() => {
        nowTime--
        if (nowTime <= 0) {
            text.value = '重置'
            clearInterval(checkFlag)
            disableButton.value = false
        } else {
            text.value =  nowTime + '后重新刷新'
        }
    }, 1000)
}

</script>

<template>
    <div>
        <page-header title="结点连接图管理" />
        <page-main>
            <div>
                <div style="display: flex;margin-bottom: 5px;justify-content: center">
                    <div style="display: flex;margin-right: 5px">
                        <div style="background-color: #79bbff; width: 50px;height: 20px;border-radius: 5px"/>
                        <div style="margin-left: 10px; color: #797979">tidb</div>
                    </div>
                    <div style="display: flex;margin-right: 5px">
                        <div style="background-color: #79ffa6; width: 50px;height: 20px;border-radius: 5px"/>
                        <div style="margin-left: 10px; color: #797979">tikv</div>
                    </div>
                    <div style="display: flex;margin-right: 5px">
                        <div style="background-color: #fa2053; width: 50px;height: 20px;border-radius: 5px"/>
                        <div style="margin-left: 10px; color: #797979">tiflash</div>
                    </div>
                    <div style="display: flex;margin-right: 5px">
                        <div style="background-color: #50eedb; width: 50px;height: 20px;border-radius: 5px"/>
                        <div style="margin-left: 10px; color: #797979">dashboard</div>
                    </div>
                    <div style="display: flex;">
                        <div style="background-color: #73767a; width: 50px;height: 20px;border-radius: 5px"/>
                        <div style="margin-left: 10px; color: #797979">其它</div>
                    </div>
                    <el-button  size="small" @click="refresh" style="margin-left: 100px" :disabled="disableButton">
                        <svg-icon  name="ep:refresh"/>
                        {{ text }}
                    </el-button>
                </div>

                <el-scrollbar ref="scrollbarRef" >
                    <div ref="innerRef" >
                        <div id="myEcharts" style="width: 2000px;height: 1000px;display: flex">
                        </div>
                    </div>
                </el-scrollbar>
            </div>
        </page-main>
    </div>
</template>

<style lang="scss" scoped>
//scss
</style>

