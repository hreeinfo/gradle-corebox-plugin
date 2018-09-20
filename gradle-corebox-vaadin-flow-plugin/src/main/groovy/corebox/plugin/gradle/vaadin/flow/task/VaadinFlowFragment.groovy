package corebox.plugin.gradle.vaadin.flow.task

/**
 *
 */
class VaadinFlowFragment {
    private String name;
    private final Set<String> files = new HashSet<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<String> getFiles() {
        return files;
    }
}
