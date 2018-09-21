package corebox.plugin.gradle.vaadin.flow.task

/**
 *
 */
class VaadinFlowFragment {
    private String name
    private final Set<String> files = new LinkedHashSet<>()

    VaadinFlowFragment() {
    }

    VaadinFlowFragment(String name, Collection<String> files) {
        this.name = name
        if (files) this.files.addAll(files)
    }


    public String getName() {
        return name
    }

    public void setName(String name) {
        this.name = name
    }

    public Set<String> getFiles() {
        return files
    }


    @Override
    public String toString() {
        return this.name + "->" + this.files
    }
}
