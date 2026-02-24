package org.nwtls.app

import org.nwtls.config.Config

fun main() {
    val cfg = Config("config.yaml")
    cfg.put("bot", "token")
    cfg.put("a.b.c.d", "some value")
    println(cfg.get("a.b.c.d"))
}
