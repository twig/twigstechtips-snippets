import sublime, sublime_plugin, json

class PrettifyJsonCommand(sublime_plugin.TextCommand):
    
    def run(self, edit):
        # if self.view.sel()[0].empty():
        #     region = sublime.Region(0L, self.view.size())
        #     source = self.view.substr(region).encode('utf-8')
        # else:
        #     source = self.view.substr(self.view.sel()[0]).encode('utf-8')
        # result = json.dumps(json.loads(source), sort_keys=True, indent=4)
        # self.view.replace(edit, self.view.sel()[0], result.decode('utf-8'))
        
        # Get the current selection
        if self.view.sel()[0].empty():
            region = sublime.Region(0L, self.view.size())
            source = self.view.substr(region).encode('utf-8')
        else:
            region = self.view.sel()[0]
            source = self.view.substr(self.view.sel()[0]).encode('utf-8')

        result = json.dumps(json.loads(source), sort_keys=True, indent=4)

        # Replace the buffer
        self.view.replace(edit, region, result.decode('utf-8'))


    def description(self):
        return "Prettify Json"
