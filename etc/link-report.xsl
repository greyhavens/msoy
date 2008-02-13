<?xml version="1.0" encoding="utf-8"?>

<!--

Stylesheet to transform a link report file from mxmlc into a readable HTML file.

Copyright 2007 Theo Hultberg / Iconara

For more info see http://blog.iconara.net/
	
-->

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                version="1.0">

	<xsl:output method="xml"
	            indent="yes"
	          encoding="UTF-8"
	       standalone="yes"
	/>
	
	<xsl:variable name="nl"><xsl:text>&#xa;</xsl:text></xsl:variable>
	
	<xsl:template match="/">
		<html>
			<head>
				<title>Project dependency analysis</title>
				
				<!-- include the inline CSS -->
				<xsl:call-template name="styles"/>
			</head>
			<body>
				<table id="link-report">
					<thead>
						<tr>
							<th>Class name</th>
							<th>References</th>
							<th>Qualified name</th>
							<th>Size</th>
						</tr>
					</thead>
					<tbody>
						<xsl:apply-templates select="/report/scripts/script" mode="table">
							<!-- generate table rows from the script nodes, sorted by size -->
							<xsl:sort order="descending" data-type="number" select="@size"/>
						</xsl:apply-templates>
					</tbody>
					<tfoot>
						<tr>
							<td></td>
							<td></td>
							<td></td>
							<td>
								<div class="size">
									<!-- output the sum of the script sizes -->
									<xsl:value-of select="format-number(sum(/report/scripts/script/@size) div 1024, '#.#')"/>
									<xsl:text> KB</xsl:text>
								</div>
							</td>
						</tr>
					</tfoot>
				</table>
			</body>
		</html>
	</xsl:template>
	
	<!--
	Generates a table row containg the description of a class.
	-->
	<xsl:template match="script" mode="table">
		<xsl:variable name="href">
			<!--
			$href will contain the path of the class' source file, or the
			SWC which contains the class.
			-->
			
			<xsl:choose>
				<xsl:when test="contains(@name, 'swc')">
					<xsl:value-of select="substring-before(@name, '(')"/>
				</xsl:when>
				<xsl:otherwise>
					<xsl:value-of select="@name"/>
				</xsl:otherwise>
			</xsl:choose>			
		</xsl:variable>
		
		<xsl:variable name="className">
			<!--
			$className will contain the name of the class, without package specifier.
			-->
			
			<xsl:choose>
				<xsl:when test="contains(def/@id, ':')">
					<xsl:value-of select="substring-after(def/@id, ':')"/>
				</xsl:when>
				<xsl:otherwise>
					<xsl:value-of select="def/@id"/>
				</xsl:otherwise>
			</xsl:choose>			
		</xsl:variable>
		
		<xsl:variable name="qualifiedName" select="def/@id"/>
		
		<tr>
			<td>
				<div class="class-name" title="{$className}">
					<a href="file://localhost{$href}"><xsl:value-of select="$className"/></a>
				</div>
			</td>
			<td>
				<xsl:value-of select="count(/report/scripts/script/dep[@id = $qualifiedName]|/report/scripts/script/pre[@id = $qualifiedName])"/>
			</td>
			<td>
				<div class="hierarchy">
					<div title="{$qualifiedName}"><xsl:value-of select="$qualifiedName"/></div>

					<ul class="prequisites">
						<xsl:apply-templates select="pre" mode="list"/>
					</ul>
					<ul class="dependencies">
						<xsl:apply-templates select="dep" mode="list"/>
					</ul>
				</div>
			</td>
			<td class="number">
				<div class="size">
					<!-- the input contains the size in bytes, convert it to KB and output with one decimal -->
					<xsl:value-of select="format-number(number(@size) div 1024, '#.#')"/>
					<xsl:text> KB</xsl:text>
				</div>
			</td>
		</tr>
	</xsl:template>
	
	<!--
	Generates a list item from either a pre or a dep node.
	The list item will have the class "dependency" or "prequisite", as well
	as "external", if the class represented by the dep/pre is an external dependency.
	-->
	<xsl:template match="dep|pre" mode="list">
		<xsl:variable name="external">
			<!--
			$external will have the value "external" if the dependency/prequisite 
			appears among the external-defs, otherwise an empty string.
			-->
			
			<xsl:variable name="local-id" select="@id"/>
			
			<xsl:choose>
				<xsl:when test="/report/external-defs/ext[@id = $local-id]">
					<xsl:text>external</xsl:text>
				</xsl:when>
				<xsl:otherwise>
					<!-- this will yield something empty -->
				</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>
		
		<xsl:variable name="type">
			<!-- $type will be either "dependency" or "prequisite" -->
			
			<xsl:choose>
				<xsl:when test="local-name() = 'dep'">
					<xsl:text>dependency</xsl:text>
				</xsl:when>
				<xsl:when test="local-name() = 'pre'">
					<xsl:text>prequisite</xsl:text>
				</xsl:when>
			</xsl:choose>
		</xsl:variable>
		
		<li class="{$type} {$external}"><xsl:value-of select="@id"/></li>
	</xsl:template>
	
	<!--
	Outputs a simple CSS to save us from the ugliness of unstyled content.
	Also hides the dependencies and prequisites, and unhides them when
	the user hovers over the qualified class name.
	-->
	<xsl:template name="styles">
		<style>
		<![CDATA[
		body {
			font-family: sans-serif;
		}
		
		#link-report {
			width: 100%;
		}
		
		td {
			vertical-align: top;
		}
		
		th {
			text-align: left;
		}
		
		tfoot {
			border-top: 1px solid black;
		}
		
		.prequisites, .dependencies {
			display: none;
			
			margin-top: 0.5em;
			padding-top: 0.5em;
			border-top: 1px solid black;
		}
		
		.hierarchy:hover .prequisites,
		.hierarchy:hover .dependencies {
			display: block;
		}
		
		.class-name {
			width: 20em;
			overflow: hidden;
		}
		
		.hierarchy {
			width: 20em;
			overflow: hidden;
		}
		
		.size {
			width: 10em;
			text-align: right;
		}
		
		.external {
			color: #999999;
		}
		]]>
		</style>
	</xsl:template>

</xsl:stylesheet>
