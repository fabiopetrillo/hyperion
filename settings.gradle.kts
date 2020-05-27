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
include("pipeline:plugins:renamer")
include("pipeline:plugins:extractor")
include("pipeline:plugins:pathextractor")
