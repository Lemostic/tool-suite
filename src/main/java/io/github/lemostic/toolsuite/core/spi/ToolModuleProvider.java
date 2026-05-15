package io.github.lemostic.toolsuite.core.spi;

import com.dlsc.workbenchfx.model.WorkbenchModule;
import io.github.lemostic.toolsuite.core.module.ModuleCategory;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public interface ToolModuleProvider {
    
    default List<Class<? extends WorkbenchModule>> getModuleClasses() {
        return List.of();
    }
    
    default String getProviderName() {
        return "Unknown Provider";
    }
    
    default String getVersion() {
        return "1.0.0";
    }
    
    default String getDescription() {
        return "";
    }
    
    default int getPriority() {
        return 100;
    }
    
    default boolean isEnabled() {
        return true;
    }
    
    default void initialize() {}
    
    default void destroy() {}
    
    class ModuleDescriptor {
        private final Class<? extends WorkbenchModule> moduleClass;
        private final String name;
        private final ModuleCategory category;
        private final String menuGroup;
        private final int menuGroupOrder;
        private final String description;
        private final String version;
        private final String author;
        private final boolean enabled;
        private final int priority;
        private final Map<String, String> metadata;
        private final Supplier<WorkbenchModule> moduleFactory;

        public ModuleDescriptor(
                Class<? extends WorkbenchModule> moduleClass,
                String name,
                ModuleCategory category,
                String menuGroup,
                int menuGroupOrder,
                String description,
                String version,
                String author,
                boolean enabled,
                int priority,
                Map<String, String> metadata,
                Supplier<WorkbenchModule> moduleFactory) {
            this.moduleClass = moduleClass;
            this.name = name;
            this.category = category;
            this.menuGroup = menuGroup;
            this.menuGroupOrder = menuGroupOrder;
            this.description = description;
            this.version = version;
            this.author = author;
            this.enabled = enabled;
            this.priority = priority;
            this.metadata = metadata;
            this.moduleFactory = moduleFactory;
        }

        public Class<? extends WorkbenchModule> getModuleClass() {
            return moduleClass;
        }

        public String getName() {
            return name;
        }

        public ModuleCategory getCategory() {
            return category;
        }

        public String getMenuGroup() {
            return menuGroup;
        }

        public int getMenuGroupOrder() {
            return menuGroupOrder;
        }

        public String getDescription() {
            return description;
        }

        public String getVersion() {
            return version;
        }

        public String getAuthor() {
            return author;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public int getPriority() {
            return priority;
        }

        public Map<String, String> getMetadata() {
            return metadata;
        }

        public WorkbenchModule createModule() {
            return moduleFactory != null ? moduleFactory.get() : null;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private Class<? extends WorkbenchModule> moduleClass;
            private String name = "";
            private ModuleCategory category = ModuleCategory.OTHERS;
            private String menuGroup = "";
            private int menuGroupOrder = 100;
            private String description = "";
            private String version = "1.0.0";
            private String author = "";
            private boolean enabled = true;
            private int priority = 100;
            private Map<String, String> metadata = Map.of();
            private Supplier<WorkbenchModule> moduleFactory;

            public Builder moduleClass(Class<? extends WorkbenchModule> moduleClass) {
                this.moduleClass = moduleClass;
                return this;
            }

            public Builder name(String name) {
                this.name = name;
                return this;
            }

            public Builder category(ModuleCategory category) {
                this.category = category;
                return this;
            }

            public Builder menuGroup(String menuGroup) {
                this.menuGroup = menuGroup;
                return this;
            }

            public Builder menuGroupOrder(int menuGroupOrder) {
                this.menuGroupOrder = menuGroupOrder;
                return this;
            }

            public Builder description(String description) {
                this.description = description;
                return this;
            }

            public Builder version(String version) {
                this.version = version;
                return this;
            }

            public Builder author(String author) {
                this.author = author;
                return this;
            }

            public Builder enabled(boolean enabled) {
                this.enabled = enabled;
                return this;
            }

            public Builder priority(int priority) {
                this.priority = priority;
                return this;
            }

            public Builder metadata(Map<String, String> metadata) {
                this.metadata = metadata;
                return this;
            }

            public Builder moduleFactory(Supplier<WorkbenchModule> factory) {
                this.moduleFactory = factory;
                return this;
            }

            public ModuleDescriptor build() {
                return new ModuleDescriptor(
                    moduleClass, name, category, menuGroup, menuGroupOrder,
                    description, version, author, enabled, priority, metadata, moduleFactory
                );
            }
        }
    }
}