export type ProjectType = 'backend' | 'frontend' | 'infra' | 'fullstack' | 'mobile' | 'multiplatform';
export type ProjectStatus = 'production' | 'development' | 'archived';

export interface ProjectModel {
  slug: string;
  name: string;
  logo?: string;
  type: ProjectType;
  description: string;
  status: ProjectStatus;
  docs?: ProjectDocsModel;
  metadata: ProjectMetadataModel;
  services?: ProjectModel[];
}

export interface ProjectDocsModel {
  overview?: ProjectOverviewData;
  product?: boolean;
  architecture?: boolean;
  features?: boolean;
  infrastructure?: boolean;
  apiDocs?: boolean;
  apiExplorer?: boolean;
  services?: boolean;
}

export interface ProjectOverviewData {
  techStack: TechStackModel[];
  highlightedFeatures: HighlightedFeatureModel[];
  highlightedCommand?: HighlightCommandModel;
}

export interface TechStackModel {
  icon: string;
  label: string;
}

export interface HighlightedFeatureModel {
  icon: string;
  label: string;
  description: string;
}

export interface HighlightCommandModel {
  title: string;
  command: string;
}

export interface ProjectMetadataModel {
  repositoryLink: string;
  deploymentLink?: string;
  license: string;
  version: string;
  recentCommits: CommitSummaryModel[];
  metrics: ProjectMetricModel[];
}

export interface ProjectMetricModel {
  label: string;
  value: number;
}

export interface CommitSummaryModel {
  hash: string;
  message: string;
  date: Date;
}
