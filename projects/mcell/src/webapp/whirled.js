if (!whirled) {
    var whirled = {};
}
var funnelHost = "www.whirled.com";

whirled.addCharts = function () {
    eval(bedrock.include({
            'bedrock.util': ['log'],
            'bedrock.iter': ['each', 'map'],
            'bedrock.collections': ['List','Dict', 'Set'],
            'panopticon.chart': ['addChart', 'init', 'StackedBarChart',
                                 'SelfContainedEventChart'],
            'panopticon.ui': ['CheckBoxes', 'RadioButtons', 'Table'],
        }));

    return function() {
        addChart("economy", "exchange_rate", "Exchange Rate", function () {
            var options = {
                xaxis: { mode: "time", minTickSize: [1, "day"]}
            };
            var chart = new SelfContainedEventChart(
                "DailyExchangeRate", function (ev, collector) {
                    collector.assume("Exchange Rate").add(
                        [ev.date, ev.rate]);
                }, options, "date");
            return chart;
        });

        addEarningsChart("coins", new List([
            [20, "Games"],              // coin rewards for playing games
            [34, "Payouts (Obsolete)"], // not used since 2008
            [40, "Badges"],             // coins from earned badges
            [51, "Payouts"],            // creator payouts for cash items
        ]));

        addEarningsChart("bars", new List([
            [50, "Bars Purchased"],
            [54, "Bling to Bars"],
            [55, "Cashed Out"],
            [57, "Subscription Bars"]
        ]));

        addEarningsChart("bling", new List([
            [51, "Payouts"],            // creator payouts for bling items
        ]));

        function addEarningsChart (currency, actionNames) {
            addChart("economy", "earnings_" + currency, "Earnings (" + currency + ")", function () {
                var actions = new CheckBoxes("Actions", "actions", actionNames);

                function valueExtractor (event, name) {
                    if (currency != event.currency || !actions.has(name)) {
                        return 0;
                    }
                    var earned = event["earned:" + name];
                    return (earned > 0) ? earned : undefined;
                }
                var chart = new StackedBarChart(
                    "DailyTransactions", actionNames, valueExtractor, {controls:[actions]});
                return chart;
            });
        }

        addChart("economy", "purchases", "Purchases", function () {
            var actionNames = new List([
                ["furniture", "Furniture"], ["avatars", "Avatars"], ["pets", "Pets"],
                ["decor", "Decor"], ["toys", "Toys"], ["games", "Games"], ["lp", "Level Packs"],
                ["ip", "Item Packs"],
            ]);
//            var actions = new CheckBoxes("Actions", "actions", actionNames);
//            var currency = new RadioButtons("Currency", "currency", ["coins", "bars", "bling" ]);
            function valueExtractor (event, name) {
//                if (currency.value != event.currency) {
//                    return 0;
//                }
//                if (!actions.has(name)) {
//                    return 0;
//                }
                return event[name] || 0;
            }
            return new StackedBarChart(
                "DailyPurchases", actionNames, valueExtractor,
                {controls:[/*actions*//*,currency*/]});
        });

        addChart("funnel", "logins", "Daily Logins", function () {
            var sourceNames = new List([
                ["uniqueGuests", "Flash Guests"],
                ["uniquePlayers", "Flash Registered"],
                ["totalPlayers", "Flash Total"],
                ["uniqueVisitors", "Flash+Web Visitors"]]);
            var sources = new CheckBoxes("Sources", "sources", sourceNames);
            var group = new RadioButtons("Group", "group", ["web", "embed" ]);
            var options = {
                xaxis: { mode: "time", minTickSize: [1, "hour"]},
                controls: [ sources, group ]
            };
            return new SelfContainedEventChart(
                "DailyLoginCount", function (ev, collector) {
                    if ((group.value == "embed") ^ ev.embed) {
                        return;
                    }
                    sourceNames.each(function (bit) {
                        if (sources.has(bit[0])) {
                            collector.assume(bit[1]).add([ev.timestamp, ev[bit[0]]]);
                        }
                    });
                }, options);
        });

        addChart("funnel", "accounts", "New Accounts (Lines)", function () {
            var sourceNames = new List([
                ["organic", "Organic"],
                ["affiliated", "Affiliated"],
                ["fromAd", "From Ad"],
                ["facebookAd", "Facebook Ad"],
                ["facebookAffiliated", "Facebook Affiliate"]]);
            var sources = new CheckBoxes("Sources", "sources", sourceNames);
            var options = {
                xaxis: { mode: "time", minTickSize: [1, "hour"]},
                controls: [ sources ]
            };

            return new SelfContainedEventChart(
                "DailyAccountsCreated", function (ev, collector) {
                    sourceNames.each(function (bit) {
                        if (sources.has(bit[0])) {
                            collector.assume(bit[1]).add([ev.date, ev[bit[0]]]);
                        }
                    });
                }, options, "date");
        });

        addChart("funnel", "accounts_stacked", "New Accounts (stacked)", function () {
            var sourceNames = new List([
                ["facebookAffiliated", "Facebook Affiliate"],
                ["fromAd", "From Ad"],
                ["facebookAd", "Facebook Ad"],
                ["affiliated", "Affiliated"],
                ["organic", "Organic"],
            ]);
            var sources = new CheckBoxes("Sources", "sources", sourceNames);
            function valueExtractor (event, name) {
                return (sources.has(name)) ? (event[name] || 0) : 0;
            }
            var chart = new StackedBarChart(
                "DailyAccountsCreated", sourceNames, valueExtractor, {controls:[sources]});
            chart.extractKey = function (ev) { return ev.date; }
            return chart;
        });

        var funnelPhases = [
            "subscribed", "paid", "retained", "returned", "registered", "played", "visited"
        ];

        var funnelGroups = [
            "GWT/Landing", "Web/Broken", "Web/Other", "GWT/Other", "Embed/Mochi", "Embed/Kongregate",
            "Embed/?Game", "Embed/?Room", "Embed/Other", "Ad/Other", "Other/Other"
        ];

        function toStacked (event, phase) {
            var cumulation = 0;
            for (var i = 0; i < funnelPhases.length; i ++) {
                if (funnelPhases[i] == phase) {
                    return Math.max(0, event[funnelPhases[i]] - cumulation);
                }
                cumulation += event[funnelPhases[i]];
            }
            log("toStacked(" + event + ", " + phase + ") returning ZERO");
        }

        addChart("funnel", "funnel_lines", "Conversion and Retention (line graph)", function () {
            var sourceNames = new List(funnelPhases);
            var sources = new CheckBoxes("Sources", "sources", sourceNames);
            var groups = new CheckBoxes("Group", "group", funnelGroups);

            var options = {
                controls: [ sources, groups ],
                xaxis: {  mode: "time", minTickSize: [1, "day"]}
            };

            var chart = new SelfContainedEventChart("funnel/date", function (ev, collector) {
                sourceNames.each(function (source) {
                    if (sources.has(source) && groups.has(ev.group)) {
                        var list = collector.assume(source);
                        var sz = list.length;
                        var arr;
                        if (sz > 0 && list.get(sz-1)[0] == ev.date) {
                            arr = list.get(sz-1);
                        } else {
                            arr = [ ev.date, 0 ];
                            list.add(arr);
                        }
                        arr[1] += ev[source];
                    }
                });
            }, options, "date");
            chart.getEvents = function (eventName, callback) {
                $.getJSON("http://" + funnelHost + "/json/" + eventName + "?jsoncallback=?",
                          callback);
            };
            return chart;
        });

        addChart("funnel", "vector_table", "Entry Vectors (table)", function () {
            function gotData (data) {
                panopticon.chart.unbindChartListeners($("#chart"));
                $("#controls").empty();
                $("#legend").empty();

                var rawOrPercent = new RadioButtons("Format", "format", ["percentage", "raw"]);
                $("#controls").append(rawOrPercent.makeHtml());
                $("#controls :input").change(function () {
                    setTimeout(drawTable, 0);
                });

                var columns = funnelPhases.slice();
                columns.reverse();
                columns.unshift("vector");

                function drawTable () {
                    // "prePlot"
                    rawOrPercent.extract();

                    // "plot"
                    $("#chart").empty().append("<div id='table'></div>");
                    $("#table").css({width: null, height: 700, overflow: "auto",
                                     border: "1px black solid"});

                    var tbl = new Table(columns, "#table", false);
                    tbl.setData = function (data) {
                        var self = this;
                        self.data = data;
                        var stripey = true;
                        $(self.selector).append(map(data, function (ev) {
                            stripey = !stripey;
                            return '<tr' +
                                (stripey ? ' class="stripe">' : '>') +
                                self.makeColumns(ev) + '</tr>';
                        }).join("\n"));
                    };
                    var events = data.events;
                    panopticon.util.sortByKey(events, "visited", true);
                    tbl.setData(map(events, function (ev) {
                        if (rawOrPercent.extract() != "percentage") {
                            return ev;
                        }
                        var result = { visited: ev.visited, vector: ev.vector };
                        for (var ii = 2; ii < columns.length; ii ++) {
                            var percent = 100 * ev[columns[ii]] / ev.visited;
                            result[columns[ii]] = percent.toFixed(1) + "%";
                        }
                        return result;
                    }));

                    // "postPlot"
                    rawOrPercent.updateFragment();
                }
                drawTable();
            }

            var eventName = "funnel/vector";
            return { run: function () {
                $.getJSON("http://" + funnelHost + "/json/" + eventName + "?jsoncallback=?", gotData);
            }};
        });
        init();
    }
}();

