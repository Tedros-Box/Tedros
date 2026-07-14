/**
 * 
 */
package org.tedros.tools.module.ai;

import org.tedros.core.TModule;
import org.tedros.core.annotation.TItem;
import org.tedros.core.annotation.TView;
import org.tedros.core.annotation.security.TAuthorizationType;
import org.tedros.core.annotation.security.TSecurity;
import org.tedros.core.domain.DomainApp;
import org.tedros.tools.ToolsKey;
import org.tedros.tools.module.ai.model.AiProviderComparisonMV;
import org.tedros.tools.module.ai.model.AiProviderComparisonModel;

/**
 * @author Davis Gordon
 *
 */
@TView(title=ToolsKey.MODULE_AI,
items = {
	@TItem(title=ToolsKey.VIEW_AI_COMPARE_MODELS, description=ToolsKey.VIEW_AI_COMPARE_MODELS_DESC,
	model = AiProviderComparisonModel.class, modelView=AiProviderComparisonMV.class, groupHeaders=true)
})
@TSecurity(	id=DomainApp.TEROS_MODULE_ID, appName=ToolsKey.APP_TOOLS, moduleName=ToolsKey.MODULE_AI, 
			allowedAccesses=TAuthorizationType.MODULE_ACCESS)
public class TAiModule extends TModule {
	
}
