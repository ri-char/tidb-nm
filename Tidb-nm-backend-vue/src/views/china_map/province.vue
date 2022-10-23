<script setup name="ProvinceName">
import 'echarts/map/js/province/zhejiang.js'

const router = useRouter()
const route = useRoute()

const province = route.params.province

let option = {
    tooltip: {
        show: false
    },
    series: [
        {
            type: "map",
            map: route.params.name,
            roam: false,
            label: {
                show: true,
                color: "#fff"
            },
            itemStyle: {
                areaColor: "#0d0059",
                borderColor: "#389dff",
                borderWidth: 0.5
            },
            emphasis: {
                areaColor: "#17008d",
                shadowOffsetX: 0,
                shadowOffsetY: 0,
                shadowBlur: 5,
                borderWidth: 0,
                shadowColor: "rgba(0, 0, 0, 0.5)"
            }
        }
    ]
}
let myEcharts
const { proxy } = getCurrentInstance()

onMounted(() => {
    myEcharts = proxy.$echarts.init(document.getElementById('map-container'));
    myEcharts.setOption(option);
})
</script>

<template>
    <div>
        <div id="map-container" ref="myEcharts" style="width: 100%;height: 460px"></div>
    </div>
</template>

<style scoped lang="scss">
//scss
</style>
