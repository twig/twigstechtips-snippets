import sublime, sublime_plugin, json

class PrettifyJsonCommand(sublime_plugin.TextCommand):
    
    def run(self, edit):

        # Get the current selection
        if self.view.sel()[0].empty():
            region = sublime.Region(0, self.view.size())
            source = self.view.substr(region)
        else:
            region = self.view.sel()[0]
            source = self.view.substr(self.view.sel()[0])
        result = json.dumps(json.loads(source), sort_keys=True, indent=4)

        # Replace the buffer
        self.view.replace(edit, region, result)


    def description(self):
        return "Prettify Json"
