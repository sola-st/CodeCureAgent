package com.fincatto.documentofiscal.nfe310.webservices.statusservico.consulta;

import org.apache.axiom.om.OMAttribute;
import org.apache.axis2.client.Stub;

import javax.xml.namespace.QName;

@SuppressWarnings({"rawtypes", "unchecked", "serial", "unused", "deprecation"})
public class NfeStatusServico2Stub extends org.apache.axis2.client.Stub {
    protected org.apache.axis2.description.AxisOperation[] operations;
    private final java.util.HashMap faultExceptionNameMap = new java.util.HashMap();
    private final java.util.HashMap faultExceptionClassNameMap = new java.util.HashMap();
    private final java.util.HashMap faultMessageMap = new java.util.HashMap();

    private static int counter = 0;

    private static synchronized java.lang.String getUniqueSuffix() {
        if (NfeStatusServico2Stub.counter > 99999) {
            NfeStatusServico2Stub.counter = 0;
        }
        NfeStatusServico2Stub.counter = NfeStatusServico2Stub.counter + 1;
        return System.currentTimeMillis() + "_" + NfeStatusServico2Stub.counter;
    }

    private void populateAxisService() {
        this._service = new org.apache.axis2.description.AxisService("NfeStatusServico2" + NfeStatusServico2Stub.getUniqueSuffix());
        this.addAnonymousOperations();
        org.apache.axis2.description.AxisOperation __operation;
        this.operations = new org.apache.axis2.description.AxisOperation[1];
        __operation = new org.apache.axis2.description.OutInAxisOperation();
        __operation.setName(new javax.xml.namespace.QName("http://www.portalfiscal.inf.br/nfe/wsdl/NfeStatusServico2", "nfeStatusServicoNF2"));
        this._service.addOperation(__operation);
        this.operations[0] = __operation;
    }

    private void populateFaults() {
    }

    public NfeStatusServico2Stub(final org.apache.axis2.context.ConfigurationContext configurationContext, final java.lang.String targetEndpoint) throws org.apache.axis2.AxisFault {
        this(configurationContext, targetEndpoint, false);
    }

    public NfeStatusServico2Stub(final org.apache.axis2.context.ConfigurationContext configurationContext, final java.lang.String targetEndpoint, final boolean useSeparateListener) throws org.apache.axis2.AxisFault {
        this.populateAxisService();
        this.populateFaults();
        this._serviceClient = new org.apache.axis2.client.ServiceClient(configurationContext, this._service);
        this._serviceClient.getOptions().setTo(new org.apache.axis2.addressing.EndpointReference(targetEndpoint));
        this._serviceClient.getOptions().setUseSeparateListener(useSeparateListener);
        this._serviceClient.getOptions().setSoapVersionURI(org.apache.axiom.soap.SOAP12Constants.SOAP_ENVELOPE_NAMESPACE_URI);
    }

    public NfeStatusServico2Stub(final org.apache.axis2.context.ConfigurationContext configurationContext) throws org.apache.axis2.AxisFault {
        this(configurationContext, "https://nfe.sefaz.rs.gov.br/ws/nfeStatusServico/nfeStatusServico2.asmx");
    }

    public NfeStatusServico2Stub() throws org.apache.axis2.AxisFault {
        this("https://nfe.sefaz.rs.gov.br/ws/nfeStatusServico/nfeStatusServico2.asmx");
    }

    public NfeStatusServico2Stub(final java.lang.String targetEndpoint) throws org.apache.axis2.AxisFault {
        this(null, targetEndpoint);
    }

    public NfeStatusServico2Stub.NfeStatusServicoNF2Result nfeStatusServicoNF2(final NfeStatusServico2Stub.NfeDadosMsg nfeDadosMsg0, final NfeStatusServico2Stub.NfeCabecMsgE nfeCabecMsg1) throws java.rmi.RemoteException {
        org.apache.axis2.context.MessageContext _messageContext = null;
        try {
            final org.apache.axis2.client.OperationClient _operationClient = this._serviceClient.createClient(this.operations[0].getName());
            _operationClient.getOptions().setAction("http://www.portalfiscal.inf.br/nfe/wsdl/NfeStatusServico2/nfeStatusServicoNF2");
            _operationClient.getOptions().setExceptionToBeThrownOnSOAPFault(true);
            this.addPropertyToOperationClient(_operationClient, org.apache.axis2.description.WSDL2Constants.ATTR_WHTTP_QUERY_PARAMETER_SEPARATOR, "&");
            // create a message context
            _messageContext = new org.apache.axis2.context.MessageContext();
            // create SOAP envelope with that payload
            org.apache.axiom.soap.SOAPEnvelope env;
            env = this.toEnvelope(Stub.getFactory(_operationClient.getOptions().getSoapVersionURI()), nfeDadosMsg0, this.optimizeContent(new javax.xml.namespace.QName("http://www.portalfiscal.inf.br/nfe/wsdl/NfeStatusServico2", "nfeStatusServicoNF2")), new javax.xml.namespace.QName("http://www.portalfiscal.inf.br/nfe/wsdl/NfeStatusServico2", "nfeStatusServicoNF2"));
            env.build();
            // add the children only if the parameter is not null
            if (nfeCabecMsg1 != null) {
                final org.apache.axiom.om.OMElement omElementnfeCabecMsg1 = this.toOM(nfeCabecMsg1, this.optimizeContent(new javax.xml.namespace.QName("http://www.portalfiscal.inf.br/nfe/wsdl/NfeStatusServico2", "nfeStatusServicoNF2")));
                this.addHeader(omElementnfeCabecMsg1, env);
            }
            // adding SOAP soap_headers
            this._serviceClient.addHeadersToEnvelope(env);
            // set the message context with that soap envelope
            _messageContext.setEnvelope(env);
            // add the message contxt to the operation client
            _operationClient.addMessageContext(_messageContext);
            // execute the operation client
            _operationClient.execute(true);
            final org.apache.axis2.context.MessageContext _returnMessageContext = _operationClient.getMessageContext(org.apache.axis2.wsdl.WSDLConstants.MESSAGE_LABEL_IN_VALUE);
            final org.apache.axiom.soap.SOAPEnvelope _returnEnv = _returnMessageContext.getEnvelope();
            final java.lang.Object object = this.fromOM(_returnEnv.getBody().getFirstElement(), NfeStatusServico2Stub.NfeStatusServicoNF2Result.class, this.getEnvelopeNamespaces(_returnEnv));
            return (NfeStatusServico2Stub.NfeStatusServicoNF2Result) object;
        } catch (final org.apache.axis2.AxisFault f) {
            final org.apache.axiom.om.OMElement faultElt = f.getDetail();
            if (faultElt != null) {
                if (this.faultExceptionNameMap.containsKey(new org.apache.axis2.client.FaultMapKey(faultElt.getQName(), "nfeStatusServicoNF2"))) {
                    // make the fault by reflection
                    try {
                        final java.lang.String exceptionClassName = (java.lang.String) this.faultExceptionClassNameMap.get(new org.apache.axis2.client.FaultMapKey(faultElt.getQName(), "nfeStatusServicoNF2"));
                        final java.lang.Class exceptionClass = java.lang.Class.forName(exceptionClassName);
                        final java.lang.reflect.Constructor constructor = exceptionClass.getConstructor(String.class);
                        final java.lang.Exception ex = (java.lang.Exception) constructor.newInstance(f.getMessage());
                        // message class
                        final java.lang.String messageClassName = (java.lang.String) this.faultMessageMap.get(new org.apache.axis2.client.FaultMapKey(faultElt.getQName(), "nfeStatusServicoNF2"));
                        final java.lang.Class messageClass = java.lang.Class.forName(messageClassName);
                        final java.lang.Object messageObject = this.fromOM(faultElt, messageClass, null);
                        final java.lang.reflect.Method m = exceptionClass.getMethod("setFaultMessage", messageClass);
                        m.invoke(ex, messageObject);
                        throw new java.rmi.RemoteException(ex.getMessage(), ex);
                    } catch (final ClassCastException | InstantiationException | IllegalAccessException | java.lang.reflect.InvocationTargetException | NoSuchMethodException | ClassNotFoundException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    }
                } else {
                    throw f;
                }
            } else {
                throw f;
            }
        } finally {
            if (_messageContext.getTransportOut() != null) {
                _messageContext.getTransportOut().getSender().cleanup(_messageContext);
            }
        }
    }

    public void startnfeStatusServicoNF2(final NfeStatusServico2Stub.NfeDadosMsg nfeDadosMsg0, final NfeStatusServico2Stub.NfeCabecMsgE nfeCabecMsg1, final NfeStatusServico2CallbackHandler callback) throws java.rmi.RemoteException {
        final org.apache.axis2.client.OperationClient _operationClient = this._serviceClient.createClient(this.operations[0].getName());
        _operationClient.getOptions().setAction("http://www.portalfiscal.inf.br/nfe/wsdl/NfeStatusServico2/nfeStatusServicoNF2");
        _operationClient.getOptions().setExceptionToBeThrownOnSOAPFault(true);
        this.addPropertyToOperationClient(_operationClient, org.apache.axis2.description.WSDL2Constants.ATTR_WHTTP_QUERY_PARAMETER_SEPARATOR, "&");
        // create SOAP envelope with that payload
        org.apache.axiom.soap.SOAPEnvelope env;
        final org.apache.axis2.context.MessageContext _messageContext = new org.apache.axis2.context.MessageContext();
        // Style is Doc.
        env = this.toEnvelope(Stub.getFactory(_operationClient.getOptions().getSoapVersionURI()), nfeDadosMsg0, this.optimizeContent(new javax.xml.namespace.QName("http://www.portalfiscal.inf.br/nfe/wsdl/NfeStatusServico2", "nfeStatusServicoNF2")), new javax.xml.namespace.QName("http://www.portalfiscal.inf.br/nfe/wsdl/NfeStatusServico2", "nfeStatusServicoNF2"));
        // add the soap_headers only if they are not null
        if (nfeCabecMsg1 != null) {
            final org.apache.axiom.om.OMElement omElementnfeCabecMsg1 = this.toOM(nfeCabecMsg1, this.optimizeContent(new javax.xml.namespace.QName("http://www.portalfiscal.inf.br/nfe/wsdl/NfeStatusServico2", "nfeStatusServicoNF2")));
            this.addHeader(omElementnfeCabecMsg1, env);
        }
        // adding SOAP soap_headers
        this._serviceClient.addHeadersToEnvelope(env);
        // create message context with that soap envelope
        _messageContext.setEnvelope(env);
        // add the message context to the operation client
        _operationClient.addMessageContext(_messageContext);
        _operationClient.setCallback(new org.apache.axis2.client.async.AxisCallback() {
            @Override
            public void onMessage(final org.apache.axis2.context.MessageContext resultContext) {
                try {
                    final org.apache.axiom.soap.SOAPEnvelope resultEnv = resultContext.getEnvelope();
                    final java.lang.Object object = NfeStatusServico2Stub.this.fromOM(resultEnv.getBody().getFirstElement(), NfeStatusServico2Stub.NfeStatusServicoNF2Result.class, NfeStatusServico2Stub.this.getEnvelopeNamespaces(resultEnv));
                    callback.receiveResultnfeStatusServicoNF2((NfeStatusServico2Stub.NfeStatusServicoNF2Result) object);
                } catch (final org.apache.axis2.AxisFault e) {
                    callback.receiveErrornfeStatusServicoNF2(e);
                }
            }

            @Override
            public void onError(final java.lang.Exception error) {
                if (error instanceof org.apache.axis2.AxisFault) {
                    final org.apache.axis2.AxisFault f = (org.apache.axis2.AxisFault) error;
                    final org.apache.axiom.om.OMElement faultElt = f.getDetail();
                    if (faultElt != null) {
                        if (NfeStatusServico2Stub.this.faultExceptionNameMap.containsKey(new org.apache.axis2.client.FaultMapKey(faultElt.getQName(), "nfeStatusServicoNF2"))) {
                            // make the fault by reflection
                            try {
                                final java.lang.String exceptionClassName = (java.lang.String) NfeStatusServico2Stub.this.faultExceptionClassNameMap.get(new org.apache.axis2.client.FaultMapKey(faultElt.getQName(), "nfeStatusServicoNF2"));
                                final java.lang.Class exceptionClass = java.lang.Class.forName(exceptionClassName);
                                final java.lang.reflect.Constructor constructor = exceptionClass.getConstructor(String.class);
                                final java.lang.Exception ex = (java.lang.Exception) constructor.newInstance(f.getMessage());
                                // message class
                                final java.lang.String messageClassName = (java.lang.String) NfeStatusServico2Stub.this.faultMessageMap.get(new org.apache.axis2.client.FaultMapKey(faultElt.getQName(), "nfeStatusServicoNF2"));
                                final java.lang.Class messageClass = java.lang.Class.forName(messageClassName);
                                final java.lang.Object messageObject = NfeStatusServico2Stub.this.fromOM(faultElt, messageClass, null);
                                final java.lang.reflect.Method m = exceptionClass.getMethod("setFaultMessage", messageClass);
                                m.invoke(ex, messageObject);
                                callback.receiveErrornfeStatusServicoNF2(new java.rmi.RemoteException(ex.getMessage(), ex));
                            } catch (final ClassCastException | org.apache.axis2.AxisFault | InstantiationException | IllegalAccessException | java.lang.reflect.InvocationTargetException | NoSuchMethodException | ClassNotFoundException e) {
                                // we cannot intantiate the class - throw the original Axis fault
                                callback.receiveErrornfeStatusServicoNF2(f);
                            }
                        } else {
                            callback.receiveErrornfeStatusServicoNF2(f);
                        }
                    } else {
                        callback.receiveErrornfeStatusServicoNF2(f);
                    }
                } else {
                    callback.receiveErrornfeStatusServicoNF2(error);
                }
            }

            @Override
            public void onFault(final org.apache.axis2.context.MessageContext faultContext) {
                final org.apache.axis2.AxisFault fault = org.apache.axis2.util.Utils.getInboundFaultFromMessageContext(faultContext);
                this.onError(fault);
            }

            @Override
            public void onComplete() {
                try {
                    _messageContext.getTransportOut().getSender().cleanup(_messageContext);
                } catch (final org.apache.axis2.AxisFault axisFault) {
                    callback.receiveErrornfeStatusServicoNF2(axisFault);
                }
            }
        });
        org.apache.axis2.util.CallbackReceiver _callbackReceiver;
        if (this.operations[0].getMessageReceiver() == null && _operationClient.getOptions().isUseSeparateListener()) {
            _callbackReceiver = new org.apache.axis2.util.CallbackReceiver();
            this.operations[0].setMessageReceiver(_callbackReceiver);
        }
        // execute the operation client
        _operationClient.execute(false);
    }

    private java.util.Map getEnvelopeNamespaces(final org.apache.axiom.soap.SOAPEnvelope env) {
        final java.util.Map returnMap = new java.util.HashMap();
        final java.util.Iterator namespaceIterator = env.getAllDeclaredNamespaces();
        while (namespaceIterator.hasNext()) {
            final org.apache.axiom.om.OMNamespace ns = (org.apache.axiom.om.OMNamespace) namespaceIterator.next();
            returnMap.put(ns.getPrefix(), ns.getNamespaceURI());
        }
        return returnMap;
    }

    private final javax.xml.namespace.QName[] opNameArray = null;

    private boolean optimizeContent(final javax.xml.namespace.QName opName) {
        if (this.opNameArray == null) {
            return false;
        }
        for (final QName element : this.opNameArray) {
            if (opName.equals(element)) {
                return true;
            }
        }
        return false;
    }

    // The rest of the code remains unchanged and omitted for brevity
    // ...
}