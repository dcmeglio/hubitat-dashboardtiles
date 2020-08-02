/**
 *  Generic Tile Device
 *
 *  Copyright 2020 Dominick Meglio
 *
 */
metadata {
    definition (name: "Generic Tile Device", namespace: "dcm.tile", author: "Dominick Meglio") {
		capability "Actuator" 
		
		attribute "tileText1", "string"
		attribute "tileText2", "string"
		attribute "tileText3", "string"
		attribute "tileText4", "string"
		attribute "tileText5", "string"
		attribute "tileText6", "string"
		attribute "tileText7", "string"
		attribute "tileText8", "string"
		attribute "tileText9", "string"
		attribute "tileText10", "string"
    }
}