import sublime, sublime_plugin, subprocess
import xml.dom.minidom
from xml.etree import ElementTree

class TidyXmlCommand(sublime_plugin.TextCommand):

    def pretty_print(self, source):
        parsed = xml.dom.minidom.parseString(source)
        lines  = parsed.toprettyxml(indent=' '*4).split('\n')
        return '\n'.join([line for line in lines if line.strip()])

    def run(self, edit):

        if self.view.sel()[0].empty():
            region = sublime.Region(0, self.view.size())
            source = self.view.substr(region).encode('utf-8')
        else:
            region = self.view.sel()[0]
            source = self.view.substr(self.view.sel()[0]).encode('utf-8')

        result = self.pretty_print(source)

        self.view.replace(edit, region, result)

    def description(self):
        return "Prettify XML"
