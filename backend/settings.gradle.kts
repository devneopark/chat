rootProject.name = "chat"

include(":modules:libs:domains:message:model")
include(":modules:libs:domains:message:event")
include(":modules:libs:domains:message:reference")

include(":modules:libs:domains:room:model")
include(":modules:libs:domains:room:event")
include(":modules:libs:domains:room:reference")

include(":modules:libs:domains:user:model")
include(":modules:libs:domains:user:event")
include(":modules:libs:domains:user:reference")
include(":modules:libs:domains:user:service")

include(":modules:libs:shared:domains:exception")
include(":modules:libs:shared:kernel")

include(":modules:services:rest-api")
include(":modules:services:messaging-gateway")
