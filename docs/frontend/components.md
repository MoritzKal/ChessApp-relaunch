# Komponenten-Inventar (v0.1)

**Primitives**

* `AppShell` (BaseFrame), `SideNav`, `AppBar`, `SnackbarHost`, `ConfirmDialog`
* `Card`, `MetricTile`, `KpiRow`, `DataTable` (Server-Pagination), `ToolbarFilters`, `EmptyState`, `ErrorState`, `LoadingState`
* `Form*` Controls: TextField, Select, Range/Slider, Switch, DateRangePicker

**Charts** (Wrapper-Komponenten)

* `TimeseriesChart` (loss/acc/req_s)
* `DistributionChart` (histogram/pie)
* `RadarChart` (Style/Opening)
* `Sparkline` (für Tiles)

**Domain**

* **Data**: `ImportForm`, `GamesTable`, `GameDetailsDrawer`
* **Datasets**: `DatasetBuilderWizard`, `DatasetList`, `DatasetDetails`
* **Training**: `TrainingRunPanel` (Tabs: Metrics | Logs | System), `TrainingWizard`
* **Models**: `ModelRegistryList`, `ModelCompare`, `PromoteDialog`
* **Evaluation**: `ABReport`, `EloSimCard`
* **Play**: `ChessBoard` (Vue-Wrapper), `MoveProbBar`, `Clock`, `StrengthControls` (Temperature/Top-k)
* **Observability**: `GrafanaPanelEmbed`, `LokiLogViewer`, `TraceLink`
