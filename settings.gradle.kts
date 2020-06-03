rootProject.name = "Hyperion"

include("aggregator")
include("datasource")
include("datasource:common")
include("datasource:plugins:elasticsearch")
include("plugin")
include("pluginmanager")
include("pipeline")
include("pipeline:common")
include("pipeline:plugins:adder")
include("pipeline:plugins:extractor")
include("pipeline:plugins:pathextractor")
include("pipeline:plugins:rate")
include("pipeline:plugins:renamer")
include("pipeline:plugins:loadbalancer")
include("pipeline:plugins:versiontracker")
include("pipeline:plugins:printer")
