package com.waicool20.wai2k.android

import org.sikuli.script.Match

@Suppress("DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE")
class AndroidMatch(match: Match, screen: AndroidScreen) : Match(match), ISikuliRegion by AndroidRegion(match, screen)
