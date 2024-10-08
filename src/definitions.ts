export interface LocationPluginPlugin {
  echo(options: { value: string }): Promise<{ value: string }>;
}
