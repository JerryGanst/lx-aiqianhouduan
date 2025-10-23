import { Module } from "@nestjs/common";

import { N8nWorkflowService } from "./services/n8n-workflow.service";

@Module({
    providers: [N8nWorkflowService],
    exports: [N8nWorkflowService],
})
export class WorkflowModule {}
