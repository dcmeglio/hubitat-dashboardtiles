/**
 *
 *  Dashboard Tile
 *
 *  Copyright 2020 Dominick Meglio
 *
 *	If you find this useful, donations are always appreciated 
 *	https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=7LBRPJRLJSDDN&source=url
 */
 
import java.util.regex.Matcher

definition(
    name: "Dashboard Tile",
    namespace: "dcm.dashboardtiles",
    author: "Dominick Meglio",
    description: "Allows you to group contact sensors together into a single virtual device",
    category: "My Apps",
	parent: "dcm.dashboardtiles:Dashboard Tiles",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
	documentationLink: "https://github.com/dcmeglio/hubitat-contactgroups/blob/master/README.md")

preferences {
    page(name: "prefContactGroup")
	page(name: "prefSettings")
}

def prefContactGroup() {
	return dynamicPage(name: "prefContactGroup", title: "Create a Dashboard Tile", nextPage: "prefSettings", uninstall:false, install: false) {
		section {
            label title: "Enter a name for this child app. This will create a virtual devidce that includes your generated dashboard tile.", required:true
		}
		displayFooter()
	}
}

def prefSettings() {
	createOrUpdateChildDevice()
    return dynamicPage(name: "prefSettings", title: "", install: true, uninstall: true) {
		section {
			paragraph "Please choose which devices to include as part of this tile and define the HTML template of the tile."

			input "devices", "capability.*", title: "Devices to monitor", multiple:true, required:true, submitOnChange: true

		    def table = "<table><tr><td>Device</td><td>Device Network Id</td><td>Id</td></tr>"

			for (dev in devices) {
				table += "<tr><td>${dev.displayName}</td><td>${dev.deviceNetworkId}</td><td>${dev.id}</td></tr>"
			}
			table += "</table>"
			paragraph table
			input "tileTemplate", "textarea", title: "Display template", required: true
       
        }
		displayFooter()
	}
}



def installed() {
	initialize()
}

def uninstalled() {
	logDebug "uninstalling app"
	for (device in getChildDevices())
	{
		deleteChildDevice(device.deviceNetworkId)
	}
}

def updated() {	
    logDebug "Updated with settings: ${settings}"
	unschedule()
    unsubscribe()
	initialize()
}

def parseVariables(template, doSubscribe) {
	template.replaceAll(/@(.+?):(.+?)@/) {
		def deviceId = it[1]
		def attribute = it[2]

		def dev = devices.find {d -> d.id == deviceId }
		if (!dev)
			return ""
		if (doSubscribe)
			subscribe(dev, attribute, eventHandler)
		return dev.currentValue(attribute)
	}
}

def parseExpressions(template) {

	template.replaceAll(/&lt;/,"<")
		.replaceAll(/&gt;/,">")
		.replaceAll(/(?ms)\<%(.+?)\%>/) {
		return evaluate(it[1])
	}
}

def initialize() {
	def outputStr = parseVariables(tileTemplate, true)
	outputStr = parseExpressions(outputStr)
	outputStr = miniMarkdownToHtml(outputStr)
	getChildDevice(state.tileDevice).sendEvent(name: "tileText1", value: outputStr)
}

def createOrUpdateChildDevice() {
	def childDevice = getChildDevice("dashboardtile:" + app.getId())
    if (!childDevice || state.tileDevice == null) {
        logDebug "Creating child device"
		state.tileDevice = "dashboardtile:" + app.getId()
		addChildDevice("dcm.tile", "Generic Tile Device", "dashboardtile:" + app.getId(), 1234, [name: app.label, isComponent: false])
    }
	else if (childDevice && childDevice.name != app.label)
		childDevice.name = app.label
}

def eventHandler(evt) {
	renderTile()
}

def renderTile() {
	def outputStr = parseVariables(tileTemplate, false)
	outputStr = parseExpressions(outputStr)
	outputStr = miniMarkdownToHtml(outputStr)
	getChildDevice(state.tileDevice).sendEvent(name: "tileText1", value: outputStr)
}



def logDebug(msg) {
    if (settings?.debugOutput) {
		log.debug msg
	}
}

def displayFooter(){
	section() {
		paragraph getFormat("line")
		paragraph "<div style='color:#1A77C9;text-align:center'>Dashboard Tiles<br><a href='https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=7LBRPJRLJSDDN&source=url' target='_blank'><img src='https://www.paypalobjects.com/webstatic/mktg/logo/pp_cc_mark_37x23.jpg' border='0' alt='PayPal Logo'></a><br><br>Please consider donating. This app took a lot of work to make.<br>If you find it valuable, I'd certainly appreciate it!</div>"
	}       
}

def getFormat(type, myText=""){			// Modified from @Stephack Code   
    if(type == "line") return "<hr style='background-color:#1A77C9; height: 1px; border: 0;'>"
    if(type == "title") return "<h2 style='color:#1A77C9;font-weight: bold'>${myText}</h2>"
}

def miniMarkdownToHtml(str) {
	def basicFormatting = str
		.replaceAll(/(?ms)-\[(.+?)\]-/, "<center>\$1</center>")
		.replaceAll(/(?m)^(?:\*\*\*+)|(?:===+)$/, "<hr>")
		.replaceAll(/(?ms)__(.+?)__/, "<b>\$1</b>")
		.replaceAll(/(?ms)\*\*(.+?)\*\*/, "<b>\$1</b>")
		.replaceAll(/(?ms)_(.+?)_/, "<i>\$1</i>")
		.replaceAll(/(?ms)\*(.+?)\*/, "<i>\$1</i>")
		.replaceAll(/(?m)^#####\s(.+)$/, "<h5>\$1</h5>")
		.replaceAll(/(?m)^####\s(.+)$/, "<h4>\$1</h4>")
		.replaceAll(/(?m)^###\s(.+)$/, "<h3>\$1</h3>")
		.replaceAll(/(?m)^##\s(.+)$/, "<h2>\$1</h2>")
		.replaceAll(/(?m)^#\s(.+)$/, "<h1>\$1</h1>")
		.replaceAll(/(?m)^(.+)\s\s$/,"\$1<br>")
		.replaceAll(/!\[.+?\]\(#(.+?)\)/, "<span style=\"font-family: 'Material Icons'\">\$1</span>")
		.replaceAll(/!\[.+?\]\((.+?) =(\d+)x(\d+)\)/, "<img src='\$1' height='\$2' width='\$3'>")
		.replaceAll(/!\[.+?\]\((.+?)\)/, "<img src='\$1'>")

	def lines = basicFormatting.replaceAll('\r','').split('\n')

	def inTable = false
	def inOl = false
	def inUl = false
	def output = ""
	for (line in lines) {
		switch (line) {
			// Tables
			case ~/^\|(.+)\|$/:
				if (!inTable) {
					inTable = true
					output += "<table style='width:100%'>"
				}
				output += "<tr>"
				def cells = Matcher.lastMatcher[0][1].split(/\|/)
				for (cell in cells) {
					output += "<td>${cell}</td>"
				}
				output += "</tr>"
				break
			// Bullets
			case ~/^(?:\+|\*|\-)\s(.+)$/:
				if (!inUl)
					output += "<ul>"
				output += "<li>${Matcher.lastMatcher[0][1]}</li>"
				break
			// Numbers
			case ~/\d+\.\s/:
				if (!inOl)
						output += "<ol>"
					output += "<li>${Matcher.lastMatcher[0][1]}</li>"
					break
				break
			default:
				if (inTable) {
					output += "</table>"
					inTable = false
				}
				if (inUl) {
					output += "</ul>"
					inTable = false
				}
				if (inOl) {
					output += "</ol>"
					inTable = false
				}
				output += line + '\n'
				break
		}
	}
	if (inUl)
		output += "</ul>"
	if (inOl)
		output += "</ol>"
	if (inTable)
		output += "</table>"

	return output
}