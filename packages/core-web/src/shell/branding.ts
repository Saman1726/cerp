export interface BrandingConfig {
  productName: string;
  logo?: { src: string; width: number; height: number; alt?: string };
  colors: { primary: string; primaryForeground?: string; background?: string };
}
