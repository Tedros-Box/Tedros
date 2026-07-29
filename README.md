<p align="center">
  <h1 align="center">Tedros Framework</h1>
  <p align="center">
    <strong>Plataforma de Desenvolvimento Corporativo com Integração Nativa de Inteligência Artificial</strong>
  </p>
  <p align="center">
    <a href="https://github.com/Tedros-Box/Tedros"><img alt="Java Version" src="https://img.shields.io/badge/Java-17-blue.svg"></a>
    <a href="https://github.com/Tedros-Box/Tedros"><img alt="JavaFX" src="https://img.shields.io/badge/JavaFX-17-orange.svg"></a>
    <a href="https://github.com/Tedros-Box/Tedros"><img alt="Jakarta EE" src="https://img.shields.io/badge/Jakarta%20EE-9-blueviolet.svg"></a>
    <a href="https://github.com/Tedros-Box/Tedros/blob/master/LICENSE"><img alt="License" src="https://img.shields.io/badge/License-Apache_2.0-green.svg"></a>
  </p>
</p>

## 📖 Índice
- [O que é o Tedros?](#-o-que-é-o-tedros)
- [Por que usar o Tedros?](#-por-que-usar-o-tedros)
- [O Ecossistema Tedros](#-o-ecossistema-tedros)
- [Mostre-me o Código](#-mostre-me-o-código)
  - [UI Declarativa](#1-ui-declarativa)
  - [Integração com IA (Teros)](#2-criação-de-funções-para-a-ia-teros)
  - [IA Centralizada no Backend (Tool Relay) e Observabilidade](#4-ia-centralizada-no-backend-tool-relay-e-observabilidade)
- [Quick Start](#-quick-start)
- [Módulos Base Disponíveis](#-módulos-base-disponíveis)
- [Caso de Sucesso: ONG Somos Social](#-caso-de-sucesso-ong-somos-social)
- [Documentação e Contato](#-documentação-e-contato)

---

## 🚀 O que é o Tedros?

**Tedros** é um framework completo para o desenvolvimento de aplicações desktop corporativas. Construído sobre **JavaFX 17** no lado cliente e **Apache TomEE (Jakarta EE 9)** no lado servidor, ele oferece uma plataforma robusta, escalável e internacionalizada.

O Tedros permite que você crie interfaces ricas de forma declarativa (usando anotações Java) e gerencie integrações complexas rapidamente. Seu maior diferencial é a integração nativa com o **Teros**, um agente interno do sistema que pode ser integrado com modelos avançados de IA (como OpenAI, Grok ou Gemini), permitindo interações naturais com os dados da sua aplicação. Desde a arquitetura **Tool Relay**, essa integração passou a ser **centralizada e orquestrada no backend** (EJB/TomEE): a chave de API nunca sai do servidor, e cada conversa é medida — em tokens, custo em dólares e latência — via um stack de observabilidade Prometheus/Grafana pronto para uso. O objetivo é que você foque apenas no núcleo do seu negócio (core business) e deixe a infraestrutura pesada por conta do Tedros.

---

## ✨ Por que usar o Tedros?

- **Desenvolvimento Declarativo:** Crie telas complexas, tabelas e formulários apenas anotando suas entidades e *ModelViews*. O framework cuida de todo o *binding* bidirecional entre Model e View.
- **Agente de Inteligência Artificial Integrado (Teros):** O Teros atua como o agente interno do sistema, conectando-se nativamente a LLMs avançados (OpenAI, Grok, Gemini) via langchain4j. Através de uma arquitetura modular baseada na classe `TFunction` (e, no modo centralizado, sua equivalente de servidor `TServerAiFunction`), o Tedros já vem equipado com **mais de 30 funções de IA (Skills) prontas para uso**, permitindo que o agente atue ativamente sobre os módulos de negócios e de infraestrutura:
  - **Integração Profunda com a UI (Core):** A IA possui "superpoderes" injetados na raiz do framework! Ela pode **navegar dinamicamente pela interface do usuário** abrindo telas automagicamente (`call_view`), ler os dados que o usuário está digitando no momento no formulário (`get_edited_model`), descobrir todas as permissões/módulos instalados no sistema (`lists_all_applications`) e até gerar ou exportar arquivos (PDF, XLSX, ZIP) em tempo real direto no servidor (`create_file`).
  - **Suporte de TI e DevOps:** O agente consegue buscar projetos, ler diffs de *commits* e *Merge Requests* do GitLab, consultar *issues* e horas trabalhadas no Redmine (inclusive baixando anexos), e pesquisar status de GMUDs em tempo real.
  - **Gestão de Pessoas (`app-person`):** Funções nativas para pesquisa de funcionários e clientes, listagem de cargos/status e automação na criação de entidades organizacionais.
  - **Estoque, Serviços e Documentos:** Capacidade da IA de pesquisar produtos e preços no inventário (`app-stock`), listar serviços (`app-services`), e fazer buscas avançadas ou downloads de documentos corporativos (`app-extensions`).
  - **Extensibilidade Infinita:** Além de todo este arsenal embutido, você estende o agente facilmente criando suas próprias funções para ler dados específicos ou executar ações automatizadas exclusivas do seu *core business*.
- **Tool Relay — IA Centralizada no Backend, com Custo e Uso sob Controle:** a orquestração do agente pode rodar inteiramente no servidor (EJB isolado `tdrs-ai`), mantendo a chave de API fora do cliente e adicionando **observabilidade de verdade**: dashboards Grafana de tokens, custo em USD por provider/modelo/usuário, latência do LLM e saúde do serviço, além de alertas Prometheus (erro de LLM, gasto diário acima do teto, saturação de conversas). Veja a seção [IA Centralizada no Backend (Tool Relay)](#4-ia-centralizada-no-backend-tool-relay-e-observabilidade) abaixo.
- **Arquitetura Cliente/Servidor Transparente:** Comunicação segura e direta via JNDI e EJB remoto usando `tomee/ejb` sobre HTTP/HTTPS.
- **Segurança Robusta:** Autenticação via token de acesso e autorização baseada em perfis e políticas totalmente gerenciáveis, protegendo cada chamada de método EJB.
- **Pronto para o Uso:** Módulos base de gestão de usuários, notificações, relatórios, temas e internacionalização integrados out-of-the-box.

---

## 🧩 O Ecossistema Tedros

O ecossistema é dividido em repositórios especializados para garantir uma arquitetura limpa e independente:

| Repositório / Pasta | Função Principal |
| :--- | :--- |
| **`Tedros/`** | **Núcleo do Framework:** Contém as APIs do lado cliente (`tedros-fx`, `tedros-core`), lado servidor (`tedros-server`), persistência e segurança. |
| **[`tedros-apps/`](https://github.com/Tedros-Box/tedros-apps)** | **Aplicações e Módulos Base:** Módulos corporativos prontos para extensão (Gestão de Pessoas, Extensões Geográficas, Estoque, Serviços, TI, Pedidos, Chat e Templates). O repositório também inclui manuais e *skills* prontos para serem consumidos por agentes de IA, auxiliando ativamente o desenvolvedor na criação de novos aplicativos. |
| **[`tedros-environment/`](https://github.com/Tedros-Box/tedros-environment)** | **Infraestrutura e Deploy:** Contém a orquestração `docker-compose` do ambiente completo (TomEE, Nginx com SSL, MongoDB, Redis e PostgreSQL via feature flag de banco) com suporte a *Remote Debug*, além de um stack **separado** de observabilidade (Prometheus + Grafana + Alertmanager) com dashboards prontos para o consumo de IA (tokens, custo em USD, tools, latência do LLM, saúde do relay). O ambiente é otimizado com um fluxo de build integrado (o Maven injeta os `.ear` recém-compilados diretamente nos contêineres para deploy imediato) e inclui scripts baseados em JPackage/Inno Setup para empacotar e gerar instaladores nativos (.exe) do cliente Desktop. |

---

## 💻 Arquitetura na Prática (Mostre-me o Código)

O Tedros utiliza um padrão arquitetural bem definido que separa as responsabilidades entre IA, Frontend (JavaFX) e Backend (TomEE/EJB). Veja como cada camada funciona através das classes do próprio sistema.

### 1. Funções para o Agente de IA (Teros)
O assistente Teros pode executar ações reais no sistema. Para isso, criamos classes que estendem `TFunction`.

- **`ListProductPriceAiFunction.java`**: 
  Esta classe permite que a IA consulte os preços dos produtos. Ela faz uma chamada remota (via `TEjbServiceLocator`) ao EJB `IProductPriceController` no servidor para buscar os dados. Se a requisição for bem sucedida, a função devolve um objeto `ToolCallResult` formatado, injetando instruções de sistema (`SYSTEM_INSTRUCTION`) que orientam a IA a processar as informações listadas e gerar uma resposta humanizada ao usuário.

<details>
<summary><b>Ver Código Fonte</b></summary>

```java
/**
 * 
 */
package org.tedros.samples.ai.function;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.naming.NamingException;

import org.slf4j.Logger;
import org.tedros.ai.function.TFunction;
import org.tedros.ai.function.ToolCallResult;
import org.tedros.ai.function.model.Empty;
import org.tedros.core.context.TedrosContext;
import org.tedros.core.service.remote.TEjbServiceLocator;
import org.tedros.sample.ejb.controller.IProductPriceController;
import org.tedros.sample.entity.ProductPrice;
import org.tedros.server.result.TResult;
import org.tedros.server.result.TResult.TState;
import org.tedros.util.TLoggerUtil;

/**
 * This function provides data on product prices 
 * to artificial intelligence
 * 
 * @author Davis Gordon
 *
 */
public class ListProductPriceAiFunction extends TFunction<Empty> {
	
	private static final Logger LOGGER = TLoggerUtil.getLogger(ListProductPriceAiFunction.class);
	
	public static final String NAME = "list_products_price";
	public static final String DESCRIPTION = "Lists all products prices";

	public ListProductPriceAiFunction() {
		super(NAME, DESCRIPTION, Empty.class, 
			v->{
				
				LOGGER.info("Listing all products prices");
								
				try(TEjbServiceLocator loc = TEjbServiceLocator.getInstance()) {
					IProductPriceController serv = loc.lookup(IProductPriceController.JNDI_NAME);
					TResult<List<ProductPrice>> res = serv
						.listAll(TedrosContext.getLoggedUser().getAccessToken(), ProductPrice.class);
					
					if(res.getState().equals(TState.SUCCESS) && !res.getValue().isEmpty()) {
						List<Price> lst = new ArrayList<>();
						res.getValue().forEach(p-> lst.add(new Price(p)));
						return ToolCallResult.builder()
								.message("Product prices retrieved successfully.")
								.result(Map.of(
					                    STATUS, SUCCESS,
					                    ACTION, "product_prices_listed",
					                    SYSTEM_INSTRUCTION, "Product prices listed successfully. "
					                    		+ "Do not retry again. Proceed with the user's request.",
					                    "product_prices", lst
					                ))
								.build();
					}
				} catch (NamingException e) {
					LOGGER.error(e.getMessage(), e);
					return ToolCallResult.builder()
							.message("Error listing product prices: " + e.getMessage())
							.result(Map.of(
				                    STATUS, ERROR,
				                    ACTION, "product_price_list_error",
				                    ERROR_MESSAGE, e.getMessage()
				                ))
							.build();
				}
				
				return ToolCallResult.builder()
						.message("No product prices found.")
						.result(Map.of(
			                    STATUS, ERROR,
			                    ACTION, "no_product_prices_found",
			                    ERROR_MESSAGE, "No product prices available in the system."
			                ))
						.build();
		});
	}
}
```
</details>

### 2. Frontend Declarativo (JavaFX)
O desenvolvimento das interfaces visuais é feito no lado cliente sem precisar escrever código repetitivo, focando em anotações que fazem o *binding* automático bidirecional.

- **Definição da Visão - `StockEntryMV.java`**:
  Uma classe que representa a tela "Entrada de Estoque". A mágica da UI declarativa acontece nas anotações:
  - `@TForm`: Define as propriedades globais do formulário.
  - `@TEjbService`: Faz o link automático da interface gráfica com o serviço remoto backend.
  - `@TListViewPresenter`: Cria de forma declarativa a listagem de registros, incluindo formulário de pesquisa complexa (usando `@TQuery`, `@TCondition` e `TJoin`) e paginação.
  - `@TSecurity`: Protege a visão e define quem pode criar, ler, editar ou apagar.
  - Componentes (`@TTabPane`, `@TComboBoxField`, `@THBox`): Geram os agrupamentos visuais e os *widgets* vinculados automaticamente aos campos da entidade.

<details>
<summary><b>Ver Código Fonte</b></summary>

```java
/**
 * 
 */
package org.tedros.stock.module.inventory.model;

import java.util.Date;
import java.util.Locale;

import org.tedros.core.TLanguage;
import org.tedros.core.annotation.security.TAuthorizationType;
import org.tedros.core.annotation.security.TSecurity;
import org.tedros.fx.TUsualKey;
import org.tedros.fx.annotation.control.TComboBoxField;
import org.tedros.fx.annotation.control.TLabel;
import org.tedros.fx.annotation.control.TProcess;
import org.tedros.fx.annotation.control.TTab;
import org.tedros.fx.annotation.control.TTabPane;
import org.tedros.fx.annotation.form.TForm;
import org.tedros.fx.annotation.form.TSetting;
import org.tedros.fx.annotation.layout.THBox;
import org.tedros.fx.annotation.layout.THGrow;
import org.tedros.fx.annotation.layout.TPane;
import org.tedros.fx.annotation.layout.TPriority;
import org.tedros.fx.annotation.page.TPage;
import org.tedros.fx.annotation.presenter.TBehavior;
import org.tedros.fx.annotation.presenter.TDecorator;
import org.tedros.fx.annotation.presenter.TListViewPresenter;
import org.tedros.fx.annotation.presenter.TPresenter;
import org.tedros.fx.annotation.process.TEjbService;
import org.tedros.fx.annotation.query.TCondition;
import org.tedros.fx.annotation.query.TJoin;
import org.tedros.fx.annotation.query.TOrder;
import org.tedros.fx.annotation.query.TQuery;
import org.tedros.fx.annotation.query.TTemporal;
import org.tedros.server.query.TCompareOp;
import org.tedros.stock.STCKKey;
import org.tedros.stock.domain.DomainApp;
import org.tedros.stock.ejb.controller.IEventTypeController;
import org.tedros.stock.ejb.controller.IStockEventController;
import org.tedros.stock.entity.EntryType;
import org.tedros.stock.entity.StockEntry;
import org.tedros.stock.module.inventory.setting.ResponsableSetting;

import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.layout.Priority;

/**
 * @author Davis Gordon
 *
 */
@TSetting(ResponsableSetting.class)
@TForm(header = "", showBreadcrumBar=false, scroll=false)
@TEjbService(serviceName = IStockEventController.JNDI_NAME, model=StockEntry.class)
@TListViewPresenter(listViewMinWidth=350,
	page=@TPage(serviceName = IStockEventController.JNDI_NAME,
		query = @TQuery(entity=StockEntry.class, 
			condition= { 
				@TCondition(field = "date", operator=TCompareOp.GREATER_EQ_THAN, label=TUsualKey.DATE, temporal=TTemporal.DATE),
				@TCondition(field = "name", alias="lp", operator=TCompareOp.LIKE, label=TUsualKey.LEGAL_PERSON)},
			join = {@TJoin(field = "legalPerson",  joinAlias = "lp"),
				@TJoin(field = "costCenter",  joinAlias = "cc")},
			orderBy= { @TOrder(label = TUsualKey.DATE , field = "date"),
				@TOrder(label = TUsualKey.COST_CENTER , field = "name", alias="cc"),
				@TOrder(label = TUsualKey.LEGAL_PERSON , field = "name", alias="lp")}
				),showSearch=true, showOrderBy=true),
	presenter=@TPresenter(
		decorator = @TDecorator(viewTitle=STCKKey.VIEW_STOCK_ENTRY, buildModesRadioButton=false),
		behavior=@TBehavior(runNewActionAfterSave=false)))
@TSecurity(id=DomainApp.STOCK_ENTRY_FORM_ID, appName = STCKKey.APP_STOCK,
	moduleName = STCKKey.MODULE_INVENTORY, viewName = STCKKey.VIEW_STOCK_ENTRY,
	allowedAccesses={TAuthorizationType.VIEW_ACCESS, TAuthorizationType.EDIT, 
					TAuthorizationType.SAVE, TAuthorizationType.DELETE, TAuthorizationType.NEW})
public class StockEntryMV extends StockEventMV<StockEntry> {

	@TTabPane(tabs = { 
		@TTab( text = TUsualKey.MAIN_DATA, fields={"type","observation"}),
		@TTab(text =  TUsualKey.PRODUCTS, fields={"items"})
	})
	private SimpleLongProperty id;
	
	@TLabel(text=TUsualKey.TYPE)
	@TComboBoxField(required=true,
	process=@TProcess(service = IEventTypeController.JNDI_NAME, 
	modelView=EntryTypeMV.class, query=@TQuery(entity=EntryType.class)))
	@THBox(	spacing=10, fillHeight=true,
			pane=@TPane(children={"legalPerson", "costCenter", "date", "responsable", "type"}), 
	hgrow=@THGrow(priority={@TPriority(field="type", priority=Priority.NEVER), 
			@TPriority(field="legalPerson", priority=Priority.NEVER), 
			@TPriority(field="responsable", priority=Priority.NEVER), 
			@TPriority(field="costCenter", priority=Priority.NEVER), 
			@TPriority(field="date", priority=Priority.NEVER)}))
	private SimpleObjectProperty<EntryType> type;
	
	public StockEntryMV(StockEntry entity) {
		super(entity);
		if(entity.isNew())
			date.setValue(new Date());
		String dtf = TLanguage.getLocale().equals(new Locale("pt"))
				? "em %4$td/%4$tm/%4$tY às %4$tT"
						: "on %4$tm-%4$td-%4$tY at %4$tT";
		super.formatToString("%s [%s], %s "+dtf, legalPerson, costCenter, type, date);
	}

}
```
</details>

- **Registro do Módulo - `InventoryModule.java`**:
  Agrupa as visões de inventário. Estende `TModule` e utiliza a anotação `@TView` para registrar os itens do menu (Configuração, Entradas, Saídas, Relatórios), vinculando as entidades do modelo (ex: `StockEntry.class`) às suas respectivas visões (`StockEntryMV.class`).

<details>
<summary><b>Ver Código Fonte</b></summary>

```java
/**
 * 
 */
package org.tedros.stock.module.inventory;

import org.tedros.core.TModule;
import org.tedros.core.annotation.TItem;
import org.tedros.core.annotation.TView;
import org.tedros.core.annotation.security.TAuthorizationType;
import org.tedros.core.annotation.security.TSecurity;
import org.tedros.stock.STCKKey;
import org.tedros.stock.domain.DomainApp;
import org.tedros.stock.entity.EntryType;
import org.tedros.stock.entity.OutType;
import org.tedros.stock.entity.StockConfig;
import org.tedros.stock.entity.StockEntry;
import org.tedros.stock.entity.StockOut;
import org.tedros.stock.model.InventoryReportModel;
import org.tedros.stock.module.inventory.model.ConfigMV;
import org.tedros.stock.module.inventory.model.EntryTypeMV;
import org.tedros.stock.module.inventory.model.OutTypeMV;
import org.tedros.stock.module.inventory.model.StockEntryMV;
import org.tedros.stock.module.inventory.model.StockOutMV;
import org.tedros.stock.module.report.model.InventoryReportMV;

/**
 * @author Davis Gordon
 *
 */
@TView(title=STCKKey.MODULE_INVENTORY,
items = { 
	@TItem(title = STCKKey.VIEW_STOCK_CONFIG, description=STCKKey.VIEW_STOCK_CONFIG_DESC,
	model=StockConfig.class, modelView=ConfigMV.class),
	@TItem(title = STCKKey.VIEW_STOCK_ENTRY, description=STCKKey.VIEW_STOCK_ENTRY_DESC,
	model=StockEntry.class, modelView=StockEntryMV.class),
	@TItem(title = STCKKey.VIEW_ENTRY_TYPE, description=STCKKey.VIEW_ENTRY_TYPE_DESC,
	model=EntryType.class, modelView=EntryTypeMV.class),
	@TItem(title = STCKKey.VIEW_STOCK_OUT, description=STCKKey.VIEW_STOCK_OUT_DESC,
	model=StockOut.class, modelView=StockOutMV.class),
	@TItem(title = STCKKey.VIEW_OUT_TYPE, description=STCKKey.VIEW_OUT_TYPE_DESC,
	model=OutType.class, modelView=OutTypeMV.class),
	@TItem(title = STCKKey.VIEW_INVENTORY_REPORT, description=STCKKey.VIEW_INVENTORY_REPORT_DESC,
	model=InventoryReportModel.class, modelView=InventoryReportMV.class),
})
@TSecurity(id=DomainApp.STOCK_CONFIG_MODULE_ID, 
appName = STCKKey.APP_STOCK, 
moduleName = STCKKey.MODULE_INVENTORY, 
allowedAccesses=TAuthorizationType.MODULE_ACCESS)
public class InventoryModule extends TModule {


}
```
</details>

- **Inicialização do App - `AppStart.java`**:
  Ponto de entrada do pacote. Através da anotação `@TApplication`, define quais módulos pertencem a este app (`ProductModule`, `InventoryModule`), os ícones do menu raiz, chaves de segurança e os arquivos de traduções (i18n).

<details>
<summary><b>Ver Código Fonte</b></summary>

```java
package org.tedros.stock.start;

import org.tedros.core.ITApplication;
import org.tedros.core.annotation.TApplication;
import org.tedros.core.annotation.TModule;
import org.tedros.core.annotation.TResourceBundle;
import org.tedros.core.annotation.security.TAuthorizationType;
import org.tedros.core.annotation.security.TSecurity;
import org.tedros.stock.STCKKey;
import org.tedros.stock.domain.DomainApp;
import org.tedros.stock.module.inventory.InventoryModule;
import org.tedros.stock.module.products.ProductModule;
import org.tedros.stock.resource.AppResource;

/**
 * The app start class.
 * 
 * @author Davis Dun
 * */
@TApplication(name=STCKKey.APP_STOCK, 
module = {	
	@TModule(type=ProductModule.class, 
			name=STCKKey.MODULE_PRODUCTS, 
			menu=STCKKey.MENU_STOCK, 
			description=STCKKey.MODULE_DESC_PRODUCTS,
			icon=TConstant.ICONS_FOLDER + "product.png", 
			menuIcon=TConstant.ICONS_FOLDER + "product_menu.png"),
	@TModule(type=InventoryModule.class, 
		name=STCKKey.MODULE_INVENTORY, 
		menu=STCKKey.MENU_STOCK, 
		description=STCKKey.MODULE_INVENTORY_DESC,
		icon=TConstant.ICONS_FOLDER + "inventory.png", 
		menuIcon=TConstant.ICONS_FOLDER + "inventory_menu.png")
}, packageName = "org.tedros.stock", universalUniqueIdentifier=TConstant.UUI)
@TResourceBundle(resourceName={"STCK"})
@TSecurity(id=DomainApp.MNEMONIC, 
appName = STCKKey.APP_STOCK, 
allowedAccesses=TAuthorizationType.APP_ACCESS)
public class AppStart implements ITApplication {

	@Override
	public void start() {
		AppResource.createResource();
	}

	@Override
	public void stop() {
		// Executed on exit and logout
	}


}
```
</details>

### 3. Backend e Lógica de Negócios (EJB / CDI / JPA)
O servidor TomEE processa a regra de negócios de forma escalável e segura.

- **Controlador EJB Seguro - `TStockEventController.java`**:
  A porta de entrada da API remota. Estende `TSecureEjbController` e é protegido pela anotação de interceptação `@TSecurityInterceptor`. Ele exige a passagem do `TAccessToken` para validar as permissões dinamicamente (via `@TBeanSecurity`). Ele delega a gravação para o *Service* e devolve as respostas de forma envelopada no objeto padrão `TResult`.

<details>
<summary><b>Ver Código Fonte</b></summary>

```java
/**
 * 
 */
package org.tedros.stock.server.ejb.controller;

import org.apache.commons.lang3.StringUtils;
import org.tedros.server.ejb.controller.ITSecurityController;
import org.tedros.server.ejb.controller.TSecureEjbController;
import org.tedros.server.result.TResult;
import org.tedros.server.result.TResult.TState;
import org.tedros.server.security.ITSecurity;
import org.tedros.server.security.TAccessPolicie;
import org.tedros.server.security.TAccessToken;
import org.tedros.server.security.TBeanPolicie;
import org.tedros.server.security.TBeanSecurity;
import org.tedros.server.security.TSecurityInterceptor;
import org.tedros.server.service.ITEjbService;
import org.tedros.stock.domain.DomainApp;
import org.tedros.stock.ejb.controller.IStockEventController;
import org.tedros.stock.entity.StockEvent;
import org.tedros.stock.entity.StockOut;
import org.tedros.stock.server.ejb.service.StockEventService;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

/**
 * The controller bean
 * 
 * @author Davis Dun
 *
 */
@TSecurityInterceptor
@Stateless(name="IStockEventController")
@TBeanSecurity({
	@TBeanPolicie(id = DomainApp.STOCK_ENTRY_FORM_ID, 
	policie = { TAccessPolicie.APP_ACCESS, TAccessPolicie.VIEW_ACCESS }),
	@TBeanPolicie(id = DomainApp.STOCK_OUT_FORM_ID, 
	policie = { TAccessPolicie.APP_ACCESS, TAccessPolicie.VIEW_ACCESS })
	})
@TransactionAttribute(value = TransactionAttributeType.NOT_SUPPORTED)
public class TStockEventController extends TSecureEjbController<StockEvent> implements IStockEventController, ITSecurity  {

	@EJB
	private StockEventService serv;
	
	@EJB
	private ITSecurityController securityController;
	
	@Override
	public ITEjbService<StockEvent> getService() {
		return serv;
	}
	
	@Override
	public ITSecurityController getSecurityController() {
		return securityController;
	}

	/* (non-Javadoc)
	 * @see org.tedros.server.ejb.controller.TSecureEjbController#save(org.tedros.server.security.TAccessToken, org.tedros.server.entity.ITEntity)
	 */
	@Override
	public TResult<StockEvent> save(TAccessToken token, StockEvent ev) {
		String warn = null;
		if(ev instanceof StockOut) {
			StockOut out = (StockOut) ev;
			try {
				warn = serv.validate(out);
			}catch(Exception ex) {
				return processException(token, ev, ex);
			}
		}
		try{
			StockEvent e = getService().save(ev);
			processEntity(token, e);
			return StringUtils.isNotBlank(warn) 
					? new TResult<>(TState.SUCCESS, true, warn, e) 
							: new TResult<>(TState.SUCCESS, e);
		}catch(Exception e){
			return processException(token, ev, e);
		}
	}
	
	
}
```
</details>

- **Serviço de Negócios (EJB) - `StockEventService.java`**:
  Um `@Singleton` local que gerencia o escopo transacional (`@TransactionAttribute`) e a concorrência (`@Lock`). É o responsável por orquestrar as rotinas e invocar as validações de negócios contidas no *Business Object*.

<details>
<summary><b>Ver Código Fonte</b></summary>

```java
/**
 * TEDROS  
 * 
 * TODOS OS DIREITOS RESERVADOS
 * 14/01/2014
 */
package org.tedros.stock.server.ejb.service;

import jakarta.ejb.ConcurrencyManagement;
import jakarta.ejb.ConcurrencyManagementType;
import jakarta.ejb.Lock;
import jakarta.ejb.LockType;
import jakarta.ejb.Singleton;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;

import org.tedros.server.cdi.bo.ITGenericBO;
import org.tedros.server.ejb.service.TEjbService;
import org.tedros.server.exception.TBusinessException;
import org.tedros.stock.entity.StockEvent;
import org.tedros.stock.entity.StockOut;
import org.tedros.stock.server.cdi.bo.StockEventBO;

/**
 * The transact service bean 
 *
 * @author Davis Dun
 *
 */
@Singleton
@Lock(LockType.READ) 
@ConcurrencyManagement(ConcurrencyManagementType.CONTAINER)
@TransactionAttribute(value = TransactionAttributeType.NOT_SUPPORTED)
public class StockEventService extends TEjbService<StockEvent>  {
	
	@Inject
	private StockEventBO bo;
	
	@Override
	public ITGenericBO<StockEvent> getBussinesObject() {
		return bo;
	}
	
	@Lock(LockType.WRITE) 
	public String validate(StockOut ev) throws TBusinessException{
		return bo.validate(ev);
	}

	/* (non-Javadoc)
	 * @see org.tedros.server.ejb.service.TEjbService#save(org.tedros.server.entity.ITEntity)
	 */
	@Override
	@Lock(LockType.WRITE) 
	@TransactionAttribute(value = TransactionAttributeType.REQUIRED)
	public StockEvent save(StockEvent ev) throws Exception {
		return bo.save(ev);
	}

	/* (non-Javadoc)
	 * @see org.tedros.server.ejb.service.TEjbService#remove(org.tedros.server.entity.ITEntity)
	 */
	@Override
	@Lock(LockType.WRITE) 
	@TransactionAttribute(value = TransactionAttributeType.REQUIRED)
	public void remove(StockEvent ev) throws Exception {
		bo.remove(ev);
	}
}
```
</details>

- **Regras de Negócio (CDI) - `StockEventBO.java`**:
  O coração da lógica do inventário (com ciclo de vida `@RequestScoped`). No método `validate()`, ele recebe um evento de retirada (`StockOut`) e cruza a informação com a quantidade atual do produto consultando o banco. Caso a requisição ultrapasse o limite mínimo de produtos e não haja liberação para estoque negativo configurada, o BO lança um alerta impeditivo no front-end através da `TBusinessException`.

<details>
<summary><b>Ver Código Fonte</b></summary>

```java
/**
 * 
 */
package org.tedros.stock.server.cdi.bo;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

import org.tedros.server.cdi.bo.TGenericBO;
import org.tedros.server.cdi.eao.ITGenericEAO;
import org.tedros.server.exception.TBusinessException;
import org.tedros.stock.entity.Product;
import org.tedros.stock.entity.StockConfig;
import org.tedros.stock.entity.StockConfigItem;
import org.tedros.stock.entity.StockEvent;
import org.tedros.stock.entity.StockItem;
import org.tedros.stock.entity.StockOut;
import org.tedros.stock.model.Inventory;
import org.tedros.stock.server.cdi.eao.InventoryEAO;
import org.tedros.stock.server.cdi.eao.StockEventEAO;

/**
 * The CDI business object 
 * 
 * @author Davis Dun
 *
 */
@RequestScoped
public class StockEventBO extends TGenericBO<StockEvent> {

	@Inject
	private StockEventEAO eao;
	
	@Inject
	private InventoryEAO invEao;

	@Inject
	private STCKBO<StockConfig> cfgBo;
	
	@Override
	public ITGenericEAO<StockEvent> getEao() {
		return eao;
	}
	
	public String validate(StockOut ev) throws TBusinessException{
		StockConfig cfg = null;
		try {
			StockConfig ex = new StockConfig();
			ex.setCostCenter(ev.getCostCenter());
			cfg = cfgBo.find(ex);
		}catch(Exception ex) {}
		
		// get a list of config items from output products
		List<StockConfigItem> citems = cfg != null 
				? cfg.getItems().stream()
				.filter(p->{
					return ev.getItems().stream()
							.anyMatch(x -> x.getProduct().equals(p.getProduct()));
				}).collect(Collectors.toList())
					: new ArrayList<>();
				
		// get the inventory list from output products
		List<Product> prods = new ArrayList<>();
		ev.getItems().forEach(p->prods.add(p.getProduct()));
		List<Inventory> iLst = invEao.calculate(ev.getLegalPerson(), ev.getCostCenter(), null, prods, null, null);
		
		StringBuilder sbWarn = new StringBuilder("");
		StringBuilder sbExc = new StringBuilder("");
		
		// validate 
		ev.getItems().stream().forEach(si->{
			
			Optional<StockConfigItem> cOp = citems.stream()
			.filter(x->x.getProduct().equals(si.getProduct()))
			.findFirst();
			StockConfigItem i = cOp.isPresent() ? cOp.get() : null;
			
			Boolean allowNeg = i!=null ? i.getAllowNegativeStock() : false;
			Optional<Inventory> op = iLst.stream().filter(p->{
				return p.getProdId().equals(si.getProduct().getId());
			}).findFirst();
			
			Double current = 0D;
			if(op.isPresent()) {
				current = op.get().getAmount();
				if(!si.isNew()) {
					try {
						StockItem svItem = invEao.findById(si);
						current += svItem.getAmount();
					} catch (Exception e) {}
				}
			}
			Double output = current - si.getAmount();
					
			if(i!=null && i.getMinimumAmount()>=output)
				sbWarn.append(si.getProduct().toString()+" #{reach.minimum.amount}\n");
			
			if((allowNeg==null || !allowNeg) && output<0)
				sbExc.append(si.getProduct().toString()+" #{with.insufficient.stock}\n");
		});
		
		if(sbExc.length()>0)
			throw new TBusinessException(sbExc.toString());
		
		return sbWarn.toString();
		
	}
}
```
</details>

- **Persistência de Dados (CDI) - `StockEventEAO.java`**:
  O *Entity Access Object* abstrai o repositório JPA. Estendendo `TGenericEAO`, ele intercepta eventos da persistência. Nos métodos de gancho (`beforeMerge`, `beforePersist`), ele percorre os itens da lista do evento (`e.getItems()`) e garante que a chave primária pai seja corretamente populada bidirecionalmente (`c.setEvent(e)`) antes do flush pro banco de dados.

<details>
<summary><b>Ver Código Fonte</b></summary>

```java
/**
 * 
 */
package org.tedros.stock.server.cdi.eao;

import jakarta.enterprise.context.RequestScoped;

import org.tedros.server.cdi.eao.TGenericEAO;
import org.tedros.stock.entity.StockEvent;

/**
 * @author Davis Gordon
 *
 */
@RequestScoped
public class StockEventEAO extends TGenericEAO<StockEvent> {

	@Override
	public void beforeMerge(StockEvent e) throws Exception {
		setEvent(e);
	}

	@Override
	public void beforePersist(StockEvent e) throws Exception {
		setEvent(e);
	}
	
	private void setEvent(StockEvent e) {
		if(e.getItems()!=null)
			e.getItems().forEach(c->{
				c.setEvent(e);
			});
	}
}
```
</details>

### 4. IA Centralizada no Backend (Tool Relay) e Observabilidade

Até aqui, cada função de IA (`TFunction`) roda dentro do próprio JavaFX, e é o cliente quem conversa diretamente com o provedor LLM. O padrão **Tool Relay** oferece um segundo modo, aditivo e opcional: o backend (EJB isolado `tdrs-ai`) passa a orquestrar toda a conversa, e o frontend vira só um executor de tools de UI e um renderizador de texto.

**A mágica primeiro:** para ligar o modo relay, basta uma property de sistema — `sys.ai.toolrelay.enabled=true`. Nenhuma tela, nenhuma `TFunction` e nenhum código do chatbot precisam mudar. A partir daí, a chave de API do provedor (OpenAI/Grok/Gemini) só existe no servidor, e cada turno de conversa passa a ser medido automaticamente: tokens, custo em dólares, latência e tools usadas aparecem prontos em dashboards Grafana.

```
[FE] pergunta do usuário ───────────────────► [BE: tdrs-ai]
[BE] chama o LLM com as tools de FE + BE ───► [LLM]
[LLM] pediu uma tool de BACKEND?  → BE executa e re-chama o LLM sozinho
[LLM] pediu uma tool de FRONTEND? → BE devolve a chamada pendente ao FE
[FE] executa a tool local (ex: abrir uma tela) e reenvia o resultado
[BE] retoma o loop com o LLM ──────────────► [LLM]
[LLM] resposta final ──────────────────────► [FE] (texto exibido no chat)
```

**Por debaixo dos panos:** o backend usa a API de baixo nível do langchain4j (`ChatModel.chat(ChatRequest)`, nunca `AiServices`) porque é isso que permite pausar o loop no meio, devolver a chamada pendente ao cliente e retomar depois — sem esse controle fino, uma tool de frontend (como abrir uma tela) não teria como ser executada pelo lado certo. Uma tool de backend é só uma classe CDI:

```java
@ApplicationScoped
public class ServerDateTimeFunction implements TServerAiFunction {

    public static class Empty { }

    @Override public String getName() { return "get_server_datetime"; }
    @Override public String getDescription() {
        return "Returns the current date and time on the Tedros server, with the server time zone.";
    }
    @Override public Class<?> getModel() { return Empty.class; }

    @Override
    public Object execute(Object arg, TAiToolContext ctx) {
        // ctx traz o TUser e o TAccessToken do turno — tools respeitam autorização
        ZonedDateTime now = ZonedDateTime.now();
        return Map.of("datetime", now.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                "zone", ZoneId.systemDefault().getId());
    }
}
```

O `TServerFunctionCatalog` descobre essa classe via CDI e a soma ao catálogo de tools do turno. Se uma tool de backend tiver o **mesmo nome** de uma spec enviada pelo cliente, a de backend sempre vence — é assim que 27 `TFunction`s de consulta (pessoas, estoque, serviços, Redmine, GitLab) já foram portadas para o servidor sem alterar uma linha do frontend.

> **Nota:** o modo legado (chave de API no cliente) continua funcionando sem nenhuma mudança — o Tool Relay é uma segunda opção de arquitetura, não uma substituição obrigatória.

> **Dica Pro — observabilidade pronta:** o EAR `tdrs-ai` expõe métricas Prometheus (`GET /ai/prometheus`) e health-check (`GET /ai/status`), nunca acessíveis pela internet (bloqueados no Nginx, raspados só dentro da rede Docker). O repositório `tedros-environment` já traz um `docker-compose` separado com Prometheus + Grafana + Alertmanager e 6 dashboards provisionados: **Tokens & Custo**, **Tools**, **Latência LLM**, **Saúde do Relay**, **Consumo por Usuário** e **Custo LLM** (com o gasto em USD por provider, modelo e usuário, calculado por uma tabela de preços versionada no banco).

> **Atenção:** nenhuma métrica exportada ao Prometheus carrega `userId` ou `conversationId` como label (evita explosão de cardinalidade). O consumo por usuário — "quem gastou quanto" — fica em uma tabela Postgres (`TAI_LLM_CALL`), consultada diretamente pelo Grafana.

---

## ⚡ Quick Start

Para garantir que o fluxo de compilação automatizado funcione sem cópias manuais, mantenha os repositórios `Tedros`, `tedros-apps` e `tedros-environment` no **mesmo nível de diretório**.

Existem duas formas principais de executar o ambiente de desenvolvimento localmente, com base no repositório auxiliar `tedros-environment`:

### Opção 1: Ambiente Completo via Docker (Simula Produção)
Ideal para homologação e execução integrada de todos os serviços. Esta opção sobe contêineres para Nginx, instâncias do TomEE, MongoDB, PostgreSQL e Redis em uma rede isolada.

1. Clone o repositório do ambiente ao lado dos outros repositórios:
   ```bash
   git clone https://github.com/Tedros-Box/tedros-environment.git
   ```
2. **Gere seus certificados TLS locais** usando o utilitário `mkcert` (obrigatório para Nginx e MongoDB) e configure o JDK:
   
   *Passo a passo:*
   - Instale o **mkcert** (ex: via Chocolatey com `choco install mkcert`).
   - Abra o terminal (PowerShell) como **Administrador** e navegue até a pasta de certificados:
     ```powershell
     cd d:\GitHub\Tedros-Box\tedros-environment\docker\nginx\ssl_local
     ```
   - Instale a Autoridade Certificadora (CA) local:
     ```powershell
     mkcert -install
     ```
   - Gere os certificados:
     ```powershell
     mkcert tedros.test localhost 127.0.0.1 ::1 mongodb
     ```
   - Crie o certificado unificado para o MongoDB:
     ```powershell
     Get-Content tedros.test+4.pem, tedros.test+4-key.pem | Set-Content mongodb.pem
     ```
   - Copie a CA Root do mkcert para a pasta atual:
     ```powershell
     $caPath = mkcert -CAROOT
     Copy-Item "$caPath\rootCA.pem" -Destination .\rootCA.pem
     ```
   - Importe a CA raiz no cacerts do seu JDK:
     ```powershell
     $keytool = "C:\Program Files\Java\jdk-17\bin\keytool.exe"
     $cacerts = "C:\Program Files\Java\jdk-17\lib\security\cacerts"
     $caFile = ".\rootCA.pem"
     & $keytool -import -trustcacerts -keystore $cacerts -storepass changeit -alias mkcert-local -file $caFile -noprompt
     ```

   **Importante:** Inclua também os mapeamentos no seu arquivo `hosts` do Windows. Abra o arquivo como **Administrador** no caminho `C:\Windows\System32\drivers\etc\hosts` e adicione o seguinte conteúdo:
   ```text
   127.0.0.1       tedros.test
   127.0.0.1       www.tedros.test
   ```
3. Inicie os contêineres:
   O banco é escolhido por **feature flag** (Compose profile + `TEDROS_DB_*`). O provider atual é **PostgreSQL**.
   
   Crie/ajuste o arquivo `.env.postgres` na pasta `tedros-environment/docker` (não versione senhas reais):
   ```env
   # Uso: docker compose --profile postgres --env-file .env.postgres up -d
   TEDROS_DB_DRIVER=org.postgresql.Driver
   TEDROS_DB_URL=jdbc:postgresql://postgres:5432/tedros?stringtype=unspecified
   TEDROS_DB_USER=tdrs
   TEDROS_DB_PASSWORD=<SENHA FORTE>
   ```
   Em seguida:
   ```bash
   cd tedros-environment/docker
   docker compose --profile postgres --env-file .env.postgres up -d --build
   ```

   > **Trocar de banco no futuro (ex. Oracle):** criar serviço Compose com `profiles: ["oracle"]`, arquivo `.env.oracle` com as mesmas chaves `TEDROS_DB_*`, driver em `common/lib`, e subir com `--profile oracle --env-file .env.oracle`. Apps/EARs não mudam.

4. **Compile os projetos:** Você pode usar o script interativo `build-projects.ps1` localizado na pasta `tedros-apps` para compilar os projetos na ordem de dependência correta, ou executar manualmente `mvn clean install`. Isso fará com que os pacotes `.ear` sejam extraídos e auto-publicados nos contêineres TomEE.
   
   - **Usando o script (Recomendado):**
     1. Edite o arquivo `tedros-apps/build-projects.ps1` e ajuste os caminhos (`Path`) da variável `$projects` para refletir as pastas da sua máquina local. Verifique também se precisa ajustar ou remover o caminho apontado para o `settings.xml` do Maven na linha de comando (`-s D:\maven\...`).
     2. Se você criar um **novo aplicativo/projeto**, lembre-se de atualizar este script adicionando o novo projeto na lista, respeitando a ordem correta de dependências (por exemplo, dependentes do `app-person` devem vir abaixo dele).
     3. Execute o script no PowerShell: `.\build-projects.ps1`, selecione os projetos na interface que vai se abrir e clique em OK.
   
   - **Manualmente:**
     Entre na pasta de cada projeto (respeitando a hierarquia de dependências) e execute `mvn clean install -DskipTests`.

5. Configure seu cliente desktop na tela de login para apontar para `https://{0}/tomee/ejb` e domínio `tedros.test`.

### Opção 2: Servidor TomEE + Banco de Dados Local (Desenvolvimento Leve)
Ideal para desenvolvimento rápido e debug direto na IDE sem precisar interagir com os contêineres Docker da infraestrutura.

1. **Inicialize o Banco de Dados Local:** Na primeira vez, suba o PostgreSQL local:
   ```powershell
   cd tedros-environment/startup-database
   .\create-tedros-data.ps1 -Database postgres
   ```
2. **Inicie o Banco de Dados:** O script acima sobe o contêiner `tedros-postgres-local`. Para subir depois: `docker compose -f docker-compose-pg.yml up -d`.

   > **Credenciais de Acesso e Conexão (Desenvolvimento):**
   > - **Usuário:** `tdrs`
   > - **Senha:** `xpto`
   > - **String JDBC Postgres:** `jdbc:postgresql://localhost:5432/tedros`
   > *(As mesmas credenciais de usuário/senha são utilizadas para acessar o MongoDB local).*
3. Navegue até o projeto do servidor local embutido:
   ```bash
   cd ../server-application
   ```
4. Suba o servidor TomEE utilizando o plugin Maven Cargo (profile `db-postgres` ativo por default):
   ```bash
   mvn cargo:run
   ```
5. O servidor fará deploy automático dos `.ear` que você compilou e instalou localmente (`mvn clean install`). Na tela de login do cliente desktop, configure para usar `http://{0}:8081/tomee/ejb` (Note a porta 8081 em HTTP) e o IP `127.0.0.1`.
6. **Rodando o Cliente na IDE:** Para rodar ou debugar o cliente desktop (`startup-tedros-box`) na IDE (Eclipse/IntelliJ), execute a classe `com.tedros.TedrosLauncher`. 
   > **Dica de Console:** Para garantir que os logs apareçam no console da IDE e não sejam silenciados (comportamento padrão para evitar travamento de buffer), adicione o argumento da VM `-Dtedros.dev=true` na sua configuração de execução (*Run Configuration*).


---

## 📦 Módulos Base Disponíveis

Estes são os principais módulos de aplicação oferecidos nativamente pelo pacote `tedros-apps`, projetados para serem estendidos ou integrados aos seus próprios projetos:

- **`app-extensions`**: Módulo base para manter dados geográficos e organizacionais, como países, estados, cidades, lugares e documentos.
- **`app-person`**: Módulo central para manter dados de diferentes tipos de pessoas (Física, Jurídica, Funcionários, Voluntários, Clientes, Filantropos e outros).
- **`app-ifood-tools`**: Módulo de ferramentas para o gerenciamento de pedidos via IFood (Em desenvolvimento).
- **`app-itsupport-tools`**: Módulo de suporte a operações de empresas de TI. Inclui integração com Redmine, GitLab e gestão de Gmud (Gestão de Mudanças), além de gerenciamento de produtividade e evidências de trabalho realizado pelo time.
- **`app-samples`**: Módulo com exemplos práticos de utilização dos componentes do framework Tedros.
- **`app-services`**: Módulo focado no gerenciamento de serviços prestados.
- **`app-stock`**: Módulo completo para gerenciamento de catálogo de produtos e controle de estoque.
- **`app-template`**: Template inicial para acelerar a construção de novos aplicativos dentro do framework.

Além dos módulos de negócio acima, o núcleo do sistema já traz ferramentas embutidas de: **Tema/Customização Visual**, **Configurações/Preferências**, **Segurança de Usuários**, **Notificações de Email** e o agente de **Inteligência Artificial (Teros)**.

### Exemplo de Integração e Dependências

A arquitetura dos módulos segue uma hierarquia inteligente de dependências para maximizar o reaproveitamento de código. Veja como eles se conectam:

- `app-person` depende de `app-extensions`
- `app-itsupport-tools` depende de `app-person`
- `app-services` depende de `app-person`
- `app-stock` depende de `app-person`
- `app-ifood-tools` depende de `app-stock`

**Na prática:** Se você for desenvolver um aplicativo novo para Gerenciamento de Vendas chamado `app-sales`, basta adicionar o `app-stock` como dependência do seu projeto. Automaticamente, seu aplicativo importará toda a gestão de produtos e estoque e, por transitividade, já terá disponíveis os módulos prontos para gerenciar **pessoas** (`app-person`) e **cidades/estados/países** (`app-extensions`).

---

## 🏆 Caso de Sucesso: ONG Somos Social

O sistema Tedros está em uso produtivo, gerenciando toda a logística da ONG [Somos Social]

Iniciada de forma amadora durante a pandemia da Covid-19 distribuindo marmitas para pessoas em vulnerabilidade, a iniciativa cresceu exponencialmente. O framework Tedros acompanhou esse salto, abraçando necessidades complexas de gestão de doações, cadastro e seleção de voluntários, controle de estoque e entrada/saída de produtos, malotes e relatórios.

Atualmente, o site público da ONG está **completamente integrado via API aos serviços da Camada de Negócios (EJB)** do backend Tedros. A prova de robustez da arquitetura Tedros é que **todo o sistema roda de forma altamente funcional e estável, sem necessitar de intervenções críticas há mais de 1.5 anos**.

## Visão do Sistema

### Telas diversas
<p align="center">
  <img src="https://github.com/Tedros-Box/Tedros/blob/master/printscreen/t-menu.png" width="100%">
  <img src="https://github.com/Tedros-Box/Tedros/blob/master/printscreen/t-apps.png" width="100%">
</p>
<p align="center">
  <img src="https://github.com/Tedros-Box/Tedros/blob/master/printscreen/t-exemplo-importar-dados.png" width="100%">
  <img src="https://github.com/Tedros-Box/Tedros/blob/master/printscreen/t-multi-telas-abertas.png" width="100%">
</p>
<p align="center">
  <img src="https://github.com/Tedros-Box/Tedros/blob/master/printscreen/t-propriedades-sistema.png" width="100%">
  <img src="https://github.com/Tedros-Box/Tedros/blob/master/printscreen/t-codigo-barras-produto.png" width="100%">
</p>
<p align="center">
  <img src="https://github.com/Tedros-Box/Tedros/blob/master/printscreen/t-captura-evidencias-trabalho-em-background.png" width="100%">
  <img src="https://github.com/Tedros-Box/Tedros/blob/master/printscreen/t-tela-user-productivity.png" width="100%">
</p>

### Agente Teros em ação

<p align="center">
  <img src="https://github.com/Tedros-Box/Tedros/blob/master/printscreen/t-teros-in-action-1.png" alt="Teros em ação" width="100%">
  <img src="https://github.com/Tedros-Box/Tedros/blob/master/printscreen/t-teros-in-action-2.png" alt="Teros em ação" width="100%">
  <img src="https://github.com/Tedros-Box/Tedros/blob/master/printscreen/t-teros-in-action-3.png" alt="Teros em ação" width="100%">
  <img src="https://github.com/Tedros-Box/Tedros/blob/master/printscreen/t-teros-in-action-4.png" alt="Teros em ação" width="100%">
</p>

### Observabilidade 

<p align="center">
  <img src="https://github.com/Tedros-Box/Tedros/blob/master/printscreen/t-dashboard-tokens-custo.png" width="100%">
  <img src="https://github.com/Tedros-Box/Tedros/blob/master/printscreen/t-dashboard-consumo-por-usuario.png" width="100%">
  <img src="https://github.com/Tedros-Box/Tedros/blob/master/printscreen/t-dashboard-saude-relay.png" width="100%">
</p>

### Customização do sistema

<p align="center">
  <img src="https://github.com/Tedros-Box/Tedros/blob/master/printscreen/t-customizacao-logo-empresa.png" width="100%">
  <img src="https://github.com/Tedros-Box/Tedros/blob/master/printscreen/t-sistema-customizado.png" width="100%">  
</p>
<p align="center">
  <img src="https://github.com/Tedros-Box/Tedros/blob/master/printscreen/t-sistema-customizado-menu.png" width="100%">
  <img src="https://github.com/Tedros-Box/Tedros/blob/master/printscreen/t-sistema-customizado-tela.png" width="100%">  
</p>



---

## 📚 Documentação e Contato

- 📖 **Wiki Oficial:** [Consulte a documentação completa e os Guias no Wiki](https://github.com/Tedros-Box/tedros-apps/wiki)
- 🧠 **Arquitetura de IA:** [Documentação de Agentes e Skills](https://github.com/Tedros-Box/tedros-apps/tree/master/skills)

**Contato / Autor:**
- 📧 Email: tedrosbox@gmail.com
- 💼 LinkedIn: [Davis Gordon](https://www.linkedin.com/in/davis-gordon-dun/)
