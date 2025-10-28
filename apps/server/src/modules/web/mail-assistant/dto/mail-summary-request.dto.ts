import { Type } from "class-transformer";
import {
    ArrayMaxSize,
    ArrayMinSize,
    IsArray,
    IsBoolean,
    IsInt,
    IsOptional,
    IsString,
    Max,
    Min,
    ValidateNested,
} from "class-validator";

class MailFolderInputDto {
    @IsString()
    id: string;

    @IsString()
    name: string;
}

export class MailSummaryRequestDto {
    @IsOptional()
    @IsString()
    accessToken?: string;

    @IsOptional()
    @IsString()
    subject?: string;

    @IsOptional()
    @IsString()
    text?: string;

    @IsOptional()
    @IsString()
    mode?: string;

    @IsOptional()
    @ValidateNested({ each: true })
    @Type(() => MailFolderInputDto)
    @IsArray()
    @ArrayMinSize(1)
    @ArrayMaxSize(10)
    folders?: MailFolderInputDto[];

    @IsOptional()
    @IsInt()
    @Min(1)
    @Max(50)
    top?: number = 20;

    @IsOptional()
    @IsBoolean()
    includeRead?: boolean = false;

    @IsOptional()
    @IsString()
    modelId?: string;
}
