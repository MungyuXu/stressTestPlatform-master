$(function () {
    $("#jqGrid").jqGrid({
        url: baseURL + 'test/stressFile/list',
        datatype: "json",
        colModel: [
            {label: '文件ID', name: 'fileId', width: 30, key: true},
            // {label: '用例ID', name: 'caseId', width: 35},
            {label: '用例名称', name: 'caseName', width: 35, sortable: false},
            {
                label: '文件名称',
                name: 'originName',
                width: 100,
                sortable: false,
                formatter: function (value, options, row) {
                    if (!(getExtension(row.originName) && /^(jmx)$/.test(getExtension(row.originName).toLowerCase()))) {
                        return value;
                    }
                    return "<a href='javascript:void(0);' onclick='" +
                        "ShowRunning(" + row.fileId + ")'>" + value + "</a>";
                }
            },
            {label: '添加时间', name: 'addTime', width: 70},
            // 当前不做更新时间，页面复杂性价比不高。
            // { label: '更新时间', name: 'updateTime', width: 80 }
            {
                label: 'Chart监控',
                name: 'webchartStatus',
                width: 30,
                sortable: false,
                formatter: function (value, options, row) {
                    if (!(getExtension(row.originName) && /^(jmx)$/.test(getExtension(row.originName).toLowerCase()))) {
                        return '';
                    }
                    if (value === 0) {
                        return '<span class="label label-success">启用</span>';
                    } else if (value === 1) {
                        return '<span class="label label-danger">禁止</span>';
                    }
                }
            },
            {
                label: '测试报告',
                name: 'reportStatus',
                width: 30,
                sortable: false,
                formatter: function (value, options, row) {
                    if (!(getExtension(row.originName) && /^(jmx)$/.test(getExtension(row.originName).toLowerCase()))) {
                        return '';
                    }
                    if (value === 0) {
                        return '<span class="label label-success">启用</span>';
                    } else if (value === 1) {
                        return '<span class="label label-danger">禁止</span>';
                    }
                }
            },
            {
                label: '调试',
                name: 'debugStatus',
                width: 20,
                sortable: false,
                formatter: function (value, options, row) {
                    if (!(getExtension(row.originName) && /^(jmx)$/.test(getExtension(row.originName).toLowerCase()))) {
                        return '';
                    }
                    if (value === 1) {
                        return '<span class="label label-success">启用</span>';
                    } else if (value === 0) {
                        return '<span class="label label-danger">禁止</span>';
                    }
                }
            },
            {
                label: '持续时间',
                name: 'duration',
                width: 40,
                sortable: false
            },
            {
                label: '状态', name: 'status', width: 40, formatter: function (value, options, row) {
                    if (value === 0) {
                        return '<span class="label label-info">创建成功</span>';
                    } else if (value === 1) {
                        return '<span class="label label-warning">正在执行</span>';
                    } else if (value === 2) {
                        if (!(getExtension(row.originName) && /^(jmx)$/.test(getExtension(row.originName).toLowerCase()))) {
                            return '<span class="label label-success">同步成功</span>';
                        }
                        return '<span class="label label-success">执行成功</span>';
                    } else if (value === 3) {
                        return '<span class="label label-danger">出现异常</span>';
                    }
                }
            },
            {
                label: '执行操作', name: '', width: 100, sortable: false, formatter: function (value, options, row) {
                    var btn = '';
                    var logBtn = '';
                    if (!(getExtension(row.originName) && /^(jmx)$/.test(getExtension(row.originName).toLowerCase()))) {
                        btn = "<a href='#' class='btn btn-primary' onclick='synchronizeFile(" + row.fileId + ")' ><i class='fa fa-arrow-circle-right'></i>&nbsp;同步文件</a>";
                    } else {
                        if (row.status == 1) {
                            btn = "<a href='#' class='btn btn-danger' onclick='stopOnce(" + row.fileId + ")' ><i class='fa fa-stop-circle'></i>&nbsp;停止</a>";
                        } else {
                            btn = "<a href='#' class='btn btn-primary' onclick='runOnce(" + row.fileId + ")' ><i class='fa fa-arrow-circle-right'></i>&nbsp;启动</a>";
                        }
                    }
                    // var stopBtn = "<a href='#' class='btn btn-primary' onclick='stop(" + row.fileId + ")' ><i class='fa fa-stop'></i>&nbsp;停止</a>";
                    // var stopNowBtn = "<a href='#' class='btn btn-primary' onclick='stopNow(" + row.fileId + ")' ><i class='fa fa-times-circle'></i>&nbsp;强制停止</a>";
                    var downloadFileBtn = "&nbsp;&nbsp;<a href='" + baseURL + "test/stressFile/downloadFile/" + row.fileId + "' class='btn btn-primary'><i class='fa fa-download'></i>&nbsp;下载</a>";
                    return btn + downloadFileBtn;
                }
            }
        ],
        viewrecords: true,
        height: $(window).height() - 150,
        rowNum: 50,
        rowList: [10, 30, 50, 100, 200],
        rownumbers: true,
        rownumWidth: 25,
        autowidth: true,
        multiselect: true,
        pager: "#jqGridPager",
        jsonReader: {
            root: "page.list",
            page: "page.currPage",
            total: "page.totalPage",
            records: "page.totalCount"
        },
        prmNames: {
            page: "page",
            rows: "limit",
            order: "order"
        },
        gridComplete: function () {
            //隐藏grid底部滚动条
            $("#jqGrid").closest(".ui-jqgrid-bdiv").css({"overflow-x": "hidden"});
        }
    });
});

var vm = new Vue({
    el: '#rrapp',
    data: {
        q: {
            caseId: null
        },
        stressTestFile: {},
        title: null,
        showChart: false,
        showList: true,
        showEdit: false,
    },
    methods: {
        query: function () {
            $("#jqGrid").jqGrid('setGridParam', {
                postData: {'caseId': vm.q.caseId},
                page: 1
            }).trigger("reloadGrid");
        },
        update: function () {
            var fileIds = getSelectedRows();
            if (fileIds == null) {
                return;
            }

            vm.showList = false;
            vm.showChart = false;
            vm.showEdit = true;
            vm.title = "配置";
            if (fileIds.length > 1) {
                vm.stressTestFile.reportStatus = 0;
                vm.stressTestFile.webchartStatus = 0;
                vm.stressTestFile.debugStatus = 0;
                vm.stressTestFile.duration = 3600;
                vm.stressTestFile.fileIdList = fileIds;
            } else {
                var fileId = fileIds[0];
                $.get(baseURL + "test/stressFile/info/" + fileId, function (r) {
                    vm.stressTestFile = r.stressTestFile;
                });
            }
        },
        saveOrUpdate: function () {
            var url = (vm.stressTestFile.fileId == null && vm.stressTestFile.fileIdList == null)
                ? "test/stressFile/save" : "test/stressFile/update";
            $.ajax({
                type: "POST",
                url: baseURL + url,
                contentType: "application/json",
                data: JSON.stringify(vm.stressTestFile),
                success: function (r) {
                    if (r.code === 0) {
                        vm.reload();
                    } else {
                        alert(r.msg);
                    }
                }
            });
        },
        del: function () {
            var fileIds = getSelectedRows();
            if (fileIds == null) {
                return;
            }

            confirm('确定要删除选中的记录？', function () {
                $.ajax({
                    type: "POST",
                    url: baseURL + "test/stressFile/delete",
                    contentType: "application/json",
                    data: JSON.stringify(fileIds),
                    success: function (r) {
                        if (r.code == 0) {
                            alert('操作成功', function () {
                                vm.reload();
                            });
                        } else {
                            alert(r.msg);
                        }
                    }
                });
            });
        },
        back: function () {
            history.go(-1);
        },
        stopAll: function () {
            confirm('确定要停止所有执行中的脚本？', function () {
                $.ajax({
                    type: "POST",
                    url: baseURL + "test/stressFile/stopAll",
                    contentType: "application/json",
                    data: "",
                    success: function (r) {
                        if (r.code == 0) {
                            alert('操作成功', function () {
                                vm.reload();
                            });
                        } else {
                            alert(r.msg);
                        }
                    }
                });
            });
        },
        stopAllNow: function () {
            var fileIds = getSelectedRows();
            if (fileIds == null) {
                return;
            }
            confirm('确定要立即停止所有选中脚本？', function () {
                $.ajax({
                    type: "POST",
                    url: baseURL + "test/stressFile/stopAllNow",
                    contentType: "application/json",
                    data: JSON.stringify(fileIds),
                    success: function (r) {
                        if (r.code == 0) {
                            alert('操作成功', function () {
                                vm.reload();
                            });
                        } else {
                            alert(r.msg);
                        }
                    }
                });
            });
        },
        reload: function (event) {
            vm.showChart = false;
            vm.showList = true;
            vm.showEdit = false;
            var page = $("#jqGrid").jqGrid('getGridParam', 'page');
            $("#jqGrid").jqGrid('setGridParam', {
                postData: {'caseId': vm.q.caseId},
                page: page
            }).trigger("reloadGrid");
            // clearInterval 是自带的函数。
            clearInterval(timeTicket);
        },
        suspendEcharts: function (event) {
            clearInterval(timeTicket);
        },
        startEcharts: function (event) {
            startInterval(fileIdData);
        },
        clearEcharts: function (event) {
            clearEcharts();
        }
    }
});

function runOnce(fileIds) {
    if (!fileIds) {
        return;
    }
    $.ajax({
        type: "POST",
        url: baseURL + "test/stressFile/runOnce",
        contentType: "application/json",
        data: JSON.stringify(numberToArray(fileIds)),
        success: function (r) {
            if (r.code == 0) {
                vm.reload();
            }
            alert(r.msg, function () {
            });
        }
    });
}

function stopOnce(fileIds) {
    if (!fileIds) {
        return;
    }
    $.ajax({
        type: "POST",
        url: baseURL + "test/stressFile/stopOnce",
        contentType: "application/json",
        data: JSON.stringify(numberToArray(fileIds)),
        success: function (r) {
            if (r.code == 0) {
                vm.reload();
                alert('操作成功', function () {
                });
            } else {
                alert(r.msg);
            }
        }
    });
}

function synchronizeFile(fileIds) {
    if (!fileIds) {
        return;
    }
    confirm('确定向所有"启用的"分布式节点机推送该文件？文件越大同步时间越长', function () {
        $.ajax({
            type: "POST",
            url: baseURL + "test/stressFile/synchronizeFile",
            contentType: "application/json",
            data: JSON.stringify(numberToArray(fileIds)),
            success: function (r) {
                if (r.code == 0) {
                    vm.reload();
                    alert('操作成功', function () {
                    });
                } else {
                    alert(r.msg);
                }
            }
        });
    });
}

var timeTicket;
var responseTimeDataObj = {};
var responseTimeLegendData = [];
var throughputDataObj = {};
var throughputLegendData = [];
var networkSentDataObj = {};
var networkSentLegendData = [];
var networkReceiveDataObj = {};
var networkReceiveLegendData = [];
var successPercentageDataObj = {};
var successPercentageLegendData = [];
var errorPercentageDataObj = {};
var errorPercentageLegendData = [];
var threadCountsDataObj = {};
var threadCountsLegendData = [];
var totalCountsDataObj = {};
var totalCountsLegendData = [];
var xAxisData = [];
var fileIdData;

function startInterval(fileId) {
    // 如果是多个脚本同时运行，切换监控页面时会发生这种情况。
    if (fileIdData > 0 && fileIdData != fileId) {
        clearEcharts();
    }
    fileIdData = fileId;
    timeTicket = setInterval(function () {
        $.get(baseURL + "test/stressFile/statInfo/" + fileId, function (r) {
            var responseTimeMap = r.statInfo.responseTimesMap;
            //拿其中的一个值尝试一下，没有则不刷新option了。
            // if (Object.keys(responseTimeMap).length  === 0) {
            //     return;
            // }

            $("#howLongRunningFormat").html(r.statInfo.howLongRunningFormat);

            // 如果不是正在执行，则不再刷新前端
            if (r.statInfo.runStatus !== 1) {
                return;
            }

            var throughputMap = r.statInfo.throughputMap;
            var networkSentMap = r.statInfo.networkSentMap;
            var networkReceiveMap = r.statInfo.networkReceiveMap;
            var successPercentageMap = r.statInfo.successPercentageMap;
            var errorPercentageMap = r.statInfo.errorPercentageMap;
            var threadCountsMap = r.statInfo.threadCountsMap;
            var totalCountsMap = r.statInfo.totalCountsMap;
            xAxisData.push(new Date().toLocaleTimeString());

            var responseTimesEChartOption = getOptionLine(responseTimeMap, responseTimeLegendData, responseTimeDataObj, null);
            var getThroughputMapOption = getOptionLine(throughputMap, throughputLegendData, throughputDataObj, null);
            var networkSentMapOption = getOptionLine(networkSentMap, networkSentLegendData, networkSentDataObj, 'sent');
            var networkReceiveMapOption = getOptionLine(networkReceiveMap, networkReceiveLegendData, networkReceiveDataObj, 'received');
            var successPercentageMapOption = getOptionLine(successPercentageMap, successPercentageLegendData, successPercentageDataObj, 'successPercentage');
            var errorPercentageMapOption = getOptionLine(errorPercentageMap, errorPercentageLegendData, errorPercentageDataObj, null);
            var threadCountsMapOption = getOptionLine(threadCountsMap, threadCountsLegendData, threadCountsDataObj, null);
            var totalCountsMapOption = getOptionPie(totalCountsMap, totalCountsLegendData, totalCountsDataObj, null);

            responseTimesEChart.setOption(responseTimesEChartOption);
            throughputEChart.setOption(getThroughputMapOption);
            networkSentEChart.setOption(networkSentMapOption);
            networkReceivedEChart.setOption(networkReceiveMapOption);
            successPercentageEChart.setOption(successPercentageMapOption);
            errorPercentageEChart.setOption(errorPercentageMapOption);
            threadCountsEChart.setOption(threadCountsMapOption);
            totalCountsEChart.setOption(totalCountsMapOption);
        });
    }, 2000);
}

function ShowRunning(fileId) {
    vm.showChart = true;
    vm.showEdit = false;
    vm.showList = false;
    startInterval(fileId);
}

function clearEcharts() {
    responseTimeDataObj = {};
    responseTimeLegendData = [];
    throughputDataObj = {};
    throughputLegendData = [];
    networkSentDataObj = {};
    networkSentLegendData = [];
    networkReceiveDataObj = {};
    networkReceiveLegendData = [];
    successPercentageDataObj = {};
    successPercentageLegendData = [];
    errorPercentageDataObj = {};
    errorPercentageLegendData = [];
    threadCountsDataObj = {};
    threadCountsLegendData = [];
    totalCountsDataObj = {};
    totalCountsLegendData = [];
    xAxisData = [];

    // 清空数据
    responseTimesEChart.setOption(optionLine, true);
    throughputEChart.setOption(optionLine, true);
    networkSentEChart.setOption(optionLine, true);
    networkReceivedEChart.setOption(optionLine, true);
    successPercentageEChart.setOption(optionLine, true);
    errorPercentageEChart.setOption(optionLine, true);
    threadCountsEChart.setOption(optionLine, true);
    totalCountsEChart.setOption(optionPie, true);
}

function getOptionLine(map, legendData, dataObj, areaStyle) {
    for (var runLabel in map) {
        var runValue = map[runLabel];
        if (legendData.indexOf(runLabel) == -1) {
            legendData.push(runLabel);
        }
        if (!dataObj[runLabel]) {
            dataObj[runLabel] = [];
        }
        dataObj[runLabel].push(runValue);
    }
    var returnOption = {
        legend: {
            data: legendData
        },
        xAxis: [
            {
                data: xAxisData
            }
        ],
        series: (function () {
            var series = [];
            for (var runLabel in map) {
                var item = {
                    name: runLabel,
                    type: 'line',
                    data: dataObj[runLabel]
                }
                if (areaStyle) {
                    item.stack = '总量';
                    item.itemStyle = {normal: {areaStyle: {type: 'default'}}};
                }
                series.push(item);
            }
            return series;
        })()
    };

    if (areaStyle) {
        returnOption.calculable = true;
    }
    return returnOption;
}


function getOptionPie(map, legendData, dataObj, areaStyle) {
    // runLabel是图形的题标内容，如[某某成功总数，某某失败总数]
    // dataObj是实际的内容值，在折线图是 内容标题：内容值，在饼图是 value：内容值，name：内容标题
    // areaStyle 是用来区别areaChart  和 普通的chart，值非空即说明是areaChart
    for (var runLabel in map) {
        var runValue = map[runLabel];
        if (legendData.indexOf(runLabel) == -1) {
            legendData.push(runLabel);
        }
        dataObj[runLabel] = runValue;
    }

    var returnOption = {
        legend: {
            orient: 'vertical',
            left: 'left',
            data: legendData
        },
        series: [
            {
                name: '请求数',
                type: 'pie',
                radius: '65%',
                center: ['50%', '50%'],
                data: (function () {
                    var data = [];
                    for (var runLabel in map) {
                        var item = {
                            name: runLabel,
                            value: dataObj[runLabel]
                        }
                        data.push(item);
                    }
                    return data;
                })(),
                itemStyle: {
                    emphasis: {
                        shadowBlur: 10,
                        shadowOffsetX: 0,
                        shadowColor: 'rgba(0, 0, 0, 0.5)'
                    }
                }
            }
        ]
    };
    return returnOption;
}


setEChartSize();
var responseTimesEChart = echarts.init(document.getElementById('responseTimesChart'), 'shine');
var throughputEChart = echarts.init(document.getElementById('throughputChart'), 'shine');
var networkSentEChart = echarts.init(document.getElementById('networkSentChart'), 'shine');
var networkReceivedEChart = echarts.init(document.getElementById('networkReceivedChart'), 'shine');
var successPercentageEChart = echarts.init(document.getElementById('successPercentageChart'), 'shine');
var errorPercentageEChart = echarts.init(document.getElementById('errorPercentageChart'), 'shine');
var threadCountsEChart = echarts.init(document.getElementById('threadCountsChart'), 'shine');
var totalCountsEChart = echarts.init(document.getElementById('totalCountsChart'), 'shine');

//用于使chart自适应高度和宽度
window.onresize = function () {
    setEChartSize();
    responseTimesEChart.resize();
    throughputEChart.resize();
    networkSentEChart.resize();
    networkReceivedEChart.resize();
    successPercentageEChart.resize();
    errorPercentageEChart.resize();
    threadCountsEChart.resize();
    totalCountsEChart.resize();
};

function setEChartSize() {
    //重置容器高宽
    $("#responseTimesChart").css('width', $("#rrapp").width() * 0.95).css('height', $("#rrapp").width() / 3);
    $("#throughputChart").css('width', $("#rrapp").width() * 0.95).css('height', $("#rrapp").width() / 3);
    $("#networkSentChart").css('width', $("#rrapp").width() * 0.95).css('height', $("#rrapp").width() / 3);
    $("#networkReceivedChart").css('width', $("#rrapp").width() * 0.95).css('height', $("#rrapp").width() / 3);
    $("#successPercentageChart").css('width', $("#rrapp").width() * 0.95).css('height', $("#rrapp").width() / 3);
    $("#errorPercentageChart").css('width', $("#rrapp").width() * 0.95).css('height', $("#rrapp").width() / 3);
    $("#threadCountsChart").css('width', $("#rrapp").width() * 0.95).css('height', $("#rrapp").width() / 3);
    $("#totalCountsChart").css('width', $("#rrapp").width() * 0.95).css('height', $("#rrapp").width() / 3);
}

// 指定折线图表的配置项和数据
var optionLine = {
    tooltip: {
        trigger: 'axis'
    },
    toolbox: {
        show: true,
        orient: 'vertical',      // 布局方式，默认为水平布局，可选为：'horizontal' ¦ 'vertical'
        borderWidth: 0,            // 工具箱边框线宽，单位px，默认为0（无边框）
        padding: 5,                // 工具箱内边距，单位px，默认各方向内边距为5，
        showTitle: true,
        disableColor: '#ddd',
        effectiveColor: 'red',
        feature: {
            dataZoom: {
                show: true,
                title: {
                    dataZoom: '区域缩放',
                    dataZoomReset: '区域缩放-后退'
                }
            },
            dataView: {
                show: true,
                title: '数据视图',
                readOnly: false,
                lang: ['数据视图', '关闭', '刷新'],
            },
            magicType: {
                show: true,
                title: {
                    line: '折线图',
                    bar: '柱形图',
                    stack: '堆积'
                },
                type: ['line', 'bar', 'stack']
            },
            restore: {
                show: true,
                title: '还原',
                color: 'black'
            }
        }
    },
    dataZoom: {
        show: true,
        start: 0,
        end: 100
    },
    legend: {},
    xAxis: [
        {
            type: 'category',
            boundaryGap: true,
            data: []
        }
    ],
    yAxis: [
        {
            type: 'value'
        }
    ]
};


// 指定饼图的配置项和数据
var optionPie = {
    tooltip: {
        trigger: 'item',
        formatter: "{a} <br/>{b} : {c} ({d}%)"
    },
    legend: {},
    series : [
        {
            name: '请求总数',
            type: 'pie',
            radius : '65%',
            center: ['50%', '50%'],
            data:[],
            itemStyle: {
                emphasis: {
                    shadowBlur: 10,
                    shadowOffsetX: 0,
                    shadowColor: 'rgba(0, 0, 0, 0.5)'
                }
            }
        }
    ]
};


// 使用刚指定的配置项和数据显示图表。
responseTimesEChart.setOption(optionLine);
throughputEChart.setOption(optionLine);
networkSentEChart.setOption(optionLine);
networkReceivedEChart.setOption(optionLine);
successPercentageEChart.setOption(optionLine);
errorPercentageEChart.setOption(optionLine);
threadCountsEChart.setOption(optionLine);
totalCountsEChart.setOption(optionPie);